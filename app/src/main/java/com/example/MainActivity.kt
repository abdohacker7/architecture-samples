package com.example.appfirewall

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: AppAdapter
    private var pendingPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recycler = findViewById<RecyclerView>(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(this)

        val apps = loadInstalledApps()
        adapter = AppAdapter(apps) { pkg -> onAppSelected(pkg) }
        recycler.adapter = adapter

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            val i = Intent(this, FirewallVpnService::class.java)
            i.action = FirewallVpnService.ACTION_STOP
            startService(i)
            Toast.makeText(this, "تم إيقاف الحجب", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onAppSelected(pkg: String) {
        pendingPackage = pkg
        val prep = VpnService.prepare(this)
        if (prep != null) {
            startActivityForResult(prep, REQ_VPN)
        } else {
            startFirewall(pkg)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_VPN && resultCode == Activity.RESULT_OK) {
            pendingPackage?.let { startFirewall(it) }
        }
    }

    private fun startFirewall(pkg: String) {
        val i = Intent(this, FirewallVpnService::class.java)
        i.putExtra(FirewallVpnService.EXTRA_ALLOWED_PKG, pkg)
        startService(i)
        Toast.makeText(this, "الحجب يعمل. التطبيق المسموح: $pkg", Toast.LENGTH_LONG).show()
    }

    private fun loadInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val list = pm.queryIntentActivities(intent, 0)
        return list.map {
            AppInfo(
                it.loadLabel(pm).toString(),
                it.activityInfo.packageName,
                it.loadIcon(pm)
            )
        }.sortedBy { it.name.lowercase() }
    }

    companion object { private const val REQ_VPN = 100 }
}

data class AppInfo(val name: String, val pkg: String, val icon: android.graphics.drawable.Drawable)

class AppAdapter(
    private val data: List<AppInfo>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<AppAdapter.VH>() {
    class VH(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.icon)
        val name: TextView = v.findViewById(R.id.name)
        val pkg: TextView = v.findViewById(R.id.pkg)
    }
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val v = android.view.LayoutInflater.from(parent.context).inflate(R.layout.row_app, parent, false)
        return VH(v)
    }
    override fun onBindViewHolder(h: VH, pos: Int) {
        val a = data[pos]
        h.icon.setImageDrawable(a.icon)
        h.name.text = a.name
        h.pkg.text = a.pkg
        h.itemView.setOnClickListener { onClick(a.pkg) }
    }
    override fun getItemCount() = data.size
}
