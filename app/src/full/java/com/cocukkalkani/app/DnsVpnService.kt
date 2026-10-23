package com.cocukkalkani.app

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Yerel "yalnızca DNS" VPN'i.
 * Telefonun DNS sunucusu sahte bir adrese (10.111.222.1) ayarlanır; yalnızca bu adrese giden
 * trafik VPN'e girer. DNS sorguları AdGuard/Cloudflare filtreli DNS'e iletilir, cevaplar geri yazılır.
 * Diğer internet trafiğine dokunulmaz, hiçbir veri bize gelmez.
 */
class DnsVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.cocukkalkani.app.START"
        const val ACTION_STOP = "com.cocukkalkani.app.STOP"
        private const val VPN_ADDRESS = "10.111.222.2"
        private const val VPN_DNS = "10.111.222.1"

        @Volatile
        var isRunning = false
            private set
    }

    private var tun: ParcelFileDescriptor? = null
    private var readerThread: Thread? = null
    private lateinit var workers: ExecutorService

    override fun onCreate() {
        super.onCreate()
        workers = Executors.newFixedThreadPool(8)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            stopSelf()
            return START_NOT_STICKY
        }
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        stopVpn() // sağlayıcı değiştiyse yeniden kur
        val provider = Prefs.provider(this)

        val configIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val builder = Builder()
            .setSession(getString(R.string.app_name) + " • " + provider.name)
            .setMtu(1500)
            .addAddress(VPN_ADDRESS, 24)
            .addDnsServer(VPN_DNS)
            .addRoute(VPN_DNS, 32)
            .setBlocking(true)
            .setConfigureIntent(configIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) builder.setMetered(false)

        val fd = try {
            builder.establish()
        } catch (e: Exception) {
            null
        } ?: return

        tun = fd
        isRunning = true
        val upstream = listOf(provider.primary, provider.secondary)

        readerThread = Thread({ readLoop(fd, upstream) }, "dns-reader").also { it.start() }
    }

    private fun readLoop(fd: ParcelFileDescriptor, upstream: List<String>) {
        val input = FileInputStream(fd.fileDescriptor)
        val output = FileOutputStream(fd.fileDescriptor)
        val buf = ByteArray(32767)
        try {
            while (!Thread.currentThread().isInterrupted) {
                val n = input.read(buf)
                if (n <= 0) continue
                val packet = buf.copyOf(n)
                workers.execute { handlePacket(packet, output, upstream) }
            }
        } catch (_: Exception) {
            // tünel kapatıldı
        }
    }

    private fun handlePacket(p: ByteArray, out: FileOutputStream, upstream: List<String>) {
        try {
            if (p.size < 28) return
            if ((p[0].toInt() shr 4) and 0xF != 4) return            // yalnızca IPv4
            val ihl = (p[0].toInt() and 0xF) * 4
            if (p[9].toInt() and 0xFF != 17) return                  // yalnızca UDP
            if (p.size < ihl + 8) return
            val totalLen = minOf(u16(p, 2), p.size)
            val srcPort = u16(p, ihl)
            val dstPort = u16(p, ihl + 2)
            if (dstPort != 53) return
            val payloadStart = ihl + 8
            val payloadLen = minOf(u16(p, ihl + 4) - 8, totalLen - payloadStart)
            if (payloadLen <= 0) return
            val query = p.copyOfRange(payloadStart, payloadStart + payloadLen)

            val resp = queryUpstream(query, upstream) ?: return

            // Cevap paketi: IPv4 başlığı (20) + UDP başlığı (8) + DNS cevabı
            val len = 28 + resp.size
            val r = ByteArray(len)
            r[0] = 0x45
            put16(r, 2, len)
            put16(r, 6, 0x4000)              // Don't Fragment
            r[8] = 64                        // TTL
            r[9] = 17                        // UDP
            p.copyInto(r, 12, 16, 20)        // kaynak = sorgunun hedefi
            p.copyInto(r, 16, 12, 16)        // hedef  = sorgunun kaynağı
            put16(r, 10, ipChecksum(r, 0, 20))
            put16(r, 20, 53)
            put16(r, 22, srcPort)
            put16(r, 24, 8 + resp.size)
            put16(r, 26, 0)                  // IPv4'te UDP checksum opsiyonel
            resp.copyInto(r, 28)

            synchronized(out) { out.write(r) }
        } catch (_: Exception) {
        }
    }

    private fun queryUpstream(query: ByteArray, servers: List<String>): ByteArray? {
        for (server in servers) {
            try {
                DatagramSocket().use { s ->
                    protect(s) // bu soket VPN'e geri dönmesin
                    s.soTimeout = 4000
                    s.send(DatagramPacket(query, query.size, InetAddress.getByName(server), 53))
                    val rb = ByteArray(4096)
                    val dp = DatagramPacket(rb, rb.size)
                    s.receive(dp)
                    return rb.copyOf(dp.length)
                }
            } catch (_: Exception) {
                // sıradaki sunucuyu dene
            }
        }
        return null
    }

    private fun u16(b: ByteArray, off: Int): Int =
        ((b[off].toInt() and 0xFF) shl 8) or (b[off + 1].toInt() and 0xFF)

    private fun put16(b: ByteArray, off: Int, v: Int) {
        b[off] = ((v shr 8) and 0xFF).toByte()
        b[off + 1] = (v and 0xFF).toByte()
    }

    private fun ipChecksum(b: ByteArray, off: Int, len: Int): Int {
        var sum = 0L
        var i = off
        while (i < off + len) {
            sum += u16(b, i).toLong()
            i += 2
        }
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        return (sum.inv() and 0xFFFF).toInt()
    }

    private fun stopVpn() {
        readerThread?.interrupt()
        readerThread = null
        try {
            tun?.close()
        } catch (_: Exception) {
        }
        tun = null
        isRunning = false
    }

    override fun onRevoke() {
        // Kullanıcı VPN'i sistemden kapattı veya başka VPN açıldı
        stopVpn()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        workers.shutdownNow()
        super.onDestroy()
    }
}
