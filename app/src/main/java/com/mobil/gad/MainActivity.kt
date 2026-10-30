package com.mobil.gad

import android.app.*
import android.content.*
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class Vehicle(val id: Long, val name: String, val plate: String, val oil: String, val tax: String, val kir: String)

class MainActivity : Activity() {
    private lateinit var container: LinearLayout
    private lateinit var nameInput: EditText
    private lateinit var plateInput: EditText
    private lateinit var oilInput: EditText
    private lateinit var taxInput: EditText
    private lateinit var kirInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createChannel()
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 10)
        showScreen()
        ReminderScheduler.rescheduleAll(this)
    }

    private fun showScreen() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(244,247,251)); setPadding(18,18,18,18) }
        val header = TextView(this).apply {
            text = "MOBIL GAD\nCatatan Oli • Pajak • KIR"
            textSize = 21f; setTextColor(Color.WHITE); setPadding(18,22,18,22)
            setBackgroundColor(Color.rgb(16,43,78))
        }
        root.addView(header)
        val scroll = ScrollView(this)
        container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0,12,0,12) }
        scroll.addView(container)
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        addForm()
        addVehicles()
    }

    private fun addForm() {
        container.addView(TextView(this).apply { text = "Tambah kendaraan"; textSize = 19f; setTextColor(Color.rgb(20,50,85)); setPadding(0,8,0,8) })
        nameInput = field("Nama kendaraan (contoh: Avanza)")
        plateInput = field("Nomor polisi")
        oilInput = dateField("Jadwal ganti oli berikutnya")
        taxInput = dateField("Tanggal jatuh tempo pajak")
        kirInput = dateField("Tanggal jatuh tempo KIR")
        container.addView(Button(this).apply { text = "SIMPAN KENDARAAN"; setOnClickListener { saveVehicle() } })
        container.addView(Button(this).apply { text = "CETAK LAPORAN"; setOnClickListener { printReport() } })
        container.addView(TextView(this).apply {
            text = "Alarm otomatis pukul 09.00, tujuh hari sebelum jadwal oli, pajak, dan KIR. Izinkan notifikasi agar pengingat muncul."
            textSize = 12f; setTextColor(Color.DKGRAY); setPadding(4,8,4,8)
        })
    }

    private fun field(h: String): EditText {
        val e = EditText(this).apply { hint = h; textSize = 15f; singleLine = true }
        container.addView(e, LinearLayout.LayoutParams(-1,-2))
        return e
    }

    private fun dateField(h: String): EditText {
        val e = field(h)
        e.isFocusable = false
        e.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d -> e.setText(String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d)) },
                c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show()
        }
        return e
    }

    private fun saveVehicle() {
        val n=nameInput.text.toString().trim(); val p=plateInput.text.toString().trim()
        if(n.isEmpty() || p.isEmpty()) { toast("Isi nama kendaraan dan nomor polisi."); return }
        val v=Vehicle(System.currentTimeMillis(),n,p,oilInput.text.toString(),taxInput.text.toString(),kirInput.text.toString())
        val a=loadVehicles(); a.put(toJson(v)); saveArray(a); ReminderScheduler.scheduleFor(this,v)
        nameInput.setText(""); plateInput.setText(""); oilInput.setText(""); taxInput.setText(""); kirInput.setText("")
        toast("Data kendaraan tersimpan."); showScreen()
    }

    private fun addVehicles() {
        container.addView(TextView(this).apply { text="Daftar kendaraan"; textSize=19f; setTextColor(Color.rgb(20,50,85)); setPadding(0,18,0,8) })
        val a=loadVehicles()
        if(a.length()==0) container.addView(TextView(this).apply { text="Belum ada kendaraan. Tambahkan data di atas."; setTextColor(Color.DKGRAY) })
        for(i in 0 until a.length()) {
            val v=fromJson(a.getJSONObject(i))
            val card=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(14,12,14,12); setBackgroundColor(Color.WHITE) }
            card.addView(TextView(this).apply {
                text="${v.name} • ${v.plate}\nGanti oli: ${showDate(v.oil)}\nPajak: ${showDate(v.tax)}\nKIR: ${showDate(v.kir)}\n${statusText(v)}"
                textSize=15f; setTextColor(Color.rgb(35,45,60))
            })
            card.addView(Button(this).apply { text="HAPUS"; setOnClickListener { deleteVehicle(v.id) } })
            val lp=LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,8,0,8); container.addView(card,lp)
        }
    }

    private fun statusText(v:Vehicle):String {
        val items=listOf("Oli" to v.oil,"Pajak" to v.tax,"KIR" to v.kir)
        return items.filter{it.second.isNotBlank()}.joinToString("   |   ") { (k,d) ->
            val days=try { ((parseDate(d).time - midnight().timeInMillis)/86400000L).toInt() } catch(e:Exception){9999}
            when { days<0 -> "$k: TERLAMBAT"; days==0 -> "$k: HARI INI"; days<=7 -> "$k: $days hari lagi"; else -> "$k: $days hari lagi" }
        }.ifBlank{"Tanggal belum diisi"}
    }

    private fun deleteVehicle(id:Long) {
        val old=loadVehicles(); val next=JSONArray()
        for(i in 0 until old.length()) if(old.getJSONObject(i).getLong("id")!=id) next.put(old.getJSONObject(i))
        saveArray(next); ReminderScheduler.cancelFor(this,id); showScreen(); toast("Kendaraan dihapus.")
    }

    private fun printReport() {
        val a=loadVehicles(); val rows=StringBuilder()
        for(i in 0 until a.length()) {
            val v=fromJson(a.getJSONObject(i))
            rows.append("<tr><td>${esc(v.name)}</td><td>${esc(v.plate)}</td><td>${esc(showDate(v.oil))}</td><td>${esc(showDate(v.tax))}</td><td>${esc(showDate(v.kir))}</td></tr>")
        }
        val html="<html><head><meta charset='utf-8'><style>body{font-family:Arial;padding:20px}table{border-collapse:collapse;width:100%}td,th{border:1px solid #555;padding:8px;text-align:left}h2{text-align:center}</style></head><body><h2>MOBIL GAD</h2><p>Laporan data kendaraan</p><table><tr><th>Kendaraan</th><th>No. Polisi</th><th>Jadwal Oli</th><th>Jatuh Tempo Pajak</th><th>Jatuh Tempo KIR</th></tr>$rows</table></body></html>"
        val web=WebView(this)
        web.webViewClient=object:WebViewClient() {
            override fun onPageFinished(view:WebView,url:String) {
                val pm=getSystemService(PRINT_SERVICE) as PrintManager
                pm.print("Laporan Mobil GAD",view.createPrintDocumentAdapter("Laporan Kendaraan"),PrintAttributes.Builder().build())
            }
        }
        web.loadDataWithBaseURL(null,html,"text/HTML","UTF-8",null)
    }

    private fun loadVehicles()=try { JSONArray(getSharedPreferences("fleet",0).getString("vehicles","[]")) } catch(e:Exception){JSONArray()}
    private fun saveArray(a:JSONArray) { getSharedPreferences("fleet",0).edit().putString("vehicles",a.toString()).apply() }
    private fun toJson(v:Vehicle)=JSONObject().put("id",v.id).put("name",v.name).put("plate",v.plate).put("oil",v.oil).put("tax",v.tax).put("kir",v.kir)
    private fun fromJson(o:JSONObject)=Vehicle(o.getLong("id"),o.optString("name"),o.optString("plate"),o.optString("oil"),o.optString("tax"),o.optString("kir"))
    private fun parseDate(s:String)=SimpleDateFormat("yyyy-MM-dd",Locale.US).apply{isLenient=false}.parse(s)!!
    private fun midnight()=Calendar.getInstance().apply{set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}
    private fun showDate(s:String)=if(s.isBlank()) "-" else try{SimpleDateFormat("dd-MM-yyyy",Locale("id","ID")).format(parseDate(s))}catch(e:Exception){s}
    private fun esc(s:String)=s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    private fun createChannel() {
        if(Build.VERSION.SDK_INT>=26) {
            val ch=NotificationChannel("fleet_reminders","Pengingat kendaraan",NotificationManager.IMPORTANCE_HIGH)
            ch.description="Pengingat oli, pajak, dan KIR"
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }
}

object ReminderScheduler {
    fun scheduleFor(c:Context,v:Vehicle) { schedule(c,v.id,"Oli",v.oil); schedule(c,v.id,"Pajak",v.tax); schedule(c,v.id,"KIR",v.kir) }
    private fun schedule(c:Context,id:Long,kind:String,date:String) {
        if(date.isBlank()) return
        try {
            val due=SimpleDateFormat("yyyy-MM-dd",Locale.US).apply{isLenient=false}.parse(date)!!
            val cal=Calendar.getInstance().apply{time=due;add(Calendar.DAY_OF_YEAR,-7);set(Calendar.HOUR_OF_DAY,9);set(Calendar.MINUTE,0);set(Calendar.SECOND,0)}
            if(cal.timeInMillis<=System.currentTimeMillis()) return
            val intent=Intent(c,ReminderReceiver::class.java).putExtra("kind",kind).putExtra("id",id).putExtra("date",date)
            val request=(id.toInt()*31+kind.hashCode())
            val pi=PendingIntent.getBroadcast(c,request,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.timeInMillis,pi)
        } catch(e:Exception) {}
    }
    fun cancelFor(c:Context,id:Long) {
        for(k in listOf("Oli","Pajak","KIR")) {
            val pi=PendingIntent.getBroadcast(c,id.toInt()*31+k.hashCode(),Intent(c,ReminderReceiver::class.java),PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if(pi!=null) (c.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi)
        }
    }
    fun rescheduleAll(c:Context) {
        val a=try{JSONArray(c.getSharedPreferences("fleet",0).getString("vehicles","[]"))}catch(e:Exception){JSONArray()}
        for(i in 0 until a.length()) { val o=a.getJSONObject(i); scheduleFor(c,Vehicle(o.getLong("id"),o.optString("name"),o.optString("plate"),o.optString("oil"),o.optString("tax"),o.optString("kir"))) }
    }
}

class ReminderReceiver:BroadcastReceiver() {
    override fun onReceive(c:Context,i:Intent) {
        val kind=i.getStringExtra("kind") ?: "Kendaraan"; val id=i.getLongExtra("id",0); val date=i.getStringExtra("date") ?: ""
        val a=try{JSONArray(c.getSharedPreferences("fleet",0).getString("vehicles","[]"))}catch(e:Exception){JSONArray()}
        var label="Kendaraan"
        for(n in 0 until a.length()) { val o=a.getJSONObject(n); if(o.optLong("id")==id) label="${o.optString("name")} (${o.optString("plate")})" }
        val nm=c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val b=if(Build.VERSION.SDK_INT>=26) Notification.Builder(c,"fleet_reminders") else Notification.Builder(c)
        nm.notify(id.toInt()*31+kind.hashCode(),b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Pengingat $kind — 7 hari lagi").setContentText("$label: jatuh tempo $date").setAutoCancel(true).build())
    }
}
class BootReceiver:BroadcastReceiver() { override fun onReceive(c:Context,i:Intent) { ReminderScheduler.rescheduleAll(c) } }
