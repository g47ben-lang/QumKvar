package com.example.shmuelkum

import android.app.*
import android.app.admin.DeviceAdminReceiver
import android.content.*
import android.graphics.Typeface
import android.hardware.*
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.io.DataOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

class BootReceiver : BroadcastReceiver() { override fun onReceive(context: Context, intent: Intent) {} }
class MyDeviceAdminReceiver : DeviceAdminReceiver()

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val prefs = context.getSharedPreferences("ShmuelAlarms", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("isAlarmActive", true).apply()

            val alarmIntent = Intent(context, MathAlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pi = PendingIntent.getActivity(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(NotificationChannel("ALARM", "Alarm", NotificationManager.IMPORTANCE_HIGH))
            }
            val notif = NotificationCompat.Builder(context, "ALARM")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("קום כבר!")
                .setContentText("הגיע הזמן לקום!")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pi, true)
                .setAutoCancel(true).build()
            nm.notify(1001, notif)
            try { context.startActivity(alarmIntent) } catch(e: Exception){}
        } catch (e: Exception) {}
    }
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Settings.canDrawOverlays(this)) try { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) } catch (e: Exception) {}
        
        val rootLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF0D47A1.toInt()) }
        val topBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = ViewGroup.LayoutParams(-1, -2); gravity = Gravity.START or Gravity.CENTER_VERTICAL }
        val btnSettings = Button(this).apply { text = "⚙️ הגדרות ניהול"; setTextColor(-1); background = null; setOnClickListener { showSettingsDialog() } }
        val btnTest = Button(this).apply { text = "🧪 חלון בדיקה"; setTextColor(-1); background = null; setOnClickListener { showTestDialog() } }
        topBar.addView(btnSettings); topBar.addView(btnTest); rootLayout.addView(topBar)

        val tabLayout = TabLayout(this).apply { setBackgroundColor(0xFF1565C0.toInt()); setTabTextColors(0xAAFFFFFF.toInt(), 0xFFFFFFFF.toInt()) }
        val viewPager = ViewPager2(this).apply { layoutParams = LinearLayout.LayoutParams(-1, -1) }
        rootLayout.addView(tabLayout); rootLayout.addView(viewPager)
        setContentView(rootLayout)

        viewPager.adapter = PagerAdapter()
        TabLayoutMediator(tabLayout, viewPager) { tab, pos -> tab.text = when(pos) { 0 -> "השכמה"; 1 -> "שנ\"צ"; else -> "שבועי" } }.attach()
    }

    private fun showTestDialog() {
        AlertDialog.Builder(this).setTitle("בדיקה").setMessage("בחר מצב בדיקה:")
            .setPositiveButton("מצב אמת") { _, _ -> startActivity(Intent(this, MathAlarmActivity::class.java).apply { putExtra("TEST_MODE", true) }) }
            .setNegativeButton("ביטול", null).show()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this).setTitle("פקודת הניהול ל-ADB")
            .setMessage("adb shell dpm set-device-owner com.example.shmuelkum/.MyDeviceAdminReceiver")
            .setPositiveButton("הבנתי", null).show()
    }

    inner class PagerAdapter : RecyclerView.Adapter<PagerAdapter.PageViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val layout = LinearLayout(parent.context).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 40, 50, 40); layoutParams = ViewGroup.LayoutParams(-1, -1); gravity = Gravity.CENTER_HORIZONTAL }
            return PageViewHolder(layout)
        }
        override fun onBindViewHolder(holder: PageViewHolder, pos: Int) {
            holder.container.removeAllViews()
            if (pos == 0) {
                val tp = TimePicker(this@MainActivity).apply { setIs24HourView(true) }
                val btn = Button(this@MainActivity).apply { text = "קבע השכמה"; setOnClickListener { val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, tp.hour); set(Calendar.MINUTE, tp.minute); set(Calendar.SECOND, 0) }; if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1); setAlarm(cal) } }
                holder.container.addView(tp); holder.container.addView(btn)
            }
        }
        override fun getItemCount() = 1
        inner class PageViewHolder(val container: LinearLayout) : RecyclerView.ViewHolder(container)
    }

    private fun setAlarm(cal: Calendar) {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(this, (cal.timeInMillis % 10000).toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val showPi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(cal.timeInMillis, showPi), pi)
        Toast.makeText(this, "אזעקה הופעלה בהצלחה", Toast.LENGTH_SHORT).show()
    }
}

class MathAlarmActivity : AppCompatActivity(), SensorEventListener {
    private var ringtone: Ringtone? = null
    private lateinit var tvStatus: TextView
    private lateinit var motionProgress: ProgressBar
    private lateinit var etAnswer: EditText
    private lateinit var btnSubmit: Button
    private lateinit var sensorManager: SensorManager
    private var accel: Sensor? = null
    private var shakes = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) { setShowWhenLocked(true); setTurnScreenOn(true) }
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_FULLSCREEN)

            val rootLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setBackgroundColor(0xFF0D47A1.toInt()); setPadding(40,40,40,40) }
            val tvMotiv = TextView(this).apply { text = "קום כבר יא עצלן!\n👁️😡"; textSize = 30f; setTextColor(0xFFFFD700.toInt()); gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,50)} }
            tvStatus = TextView(this).apply { text = "נער את המכשיר!"; textSize = 24f; setTextColor(-1); gravity = Gravity.CENTER }
            motionProgress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { layoutParams = LinearLayout.LayoutParams(-1, 50).apply{setMargins(0,50,0,50)}; max = 100 }
            etAnswer = EditText(this).apply { hint = "הקלד: אני ער לגמרי"; setTextColor(-1); setHintTextColor(0x88FFFFFF.toInt()); visibility = View.GONE; gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,50,0,50)} }
            btnSubmit = Button(this).apply { text = "אישור"; visibility = View.GONE; setBackgroundColor(0xFF4CAF50.toInt()); setTextColor(-1) }
            
            rootLayout.addView(tvMotiv); rootLayout.addView(tvStatus); rootLayout.addView(motionProgress); rootLayout.addView(etAnswer); rootLayout.addView(btnSubmit)
            
            if (intent.getBooleanExtra("TEST_MODE", false)) {
                val btnEmerg = Button(this).apply { text = "🛑 יציאת חירום מבדיקה"; setBackgroundColor(0xCCFF0000.toInt()); setTextColor(-1); setOnClickListener { finishAlarm() } }
                rootLayout.addView(btnEmerg)
            }
            setContentView(rootLayout)

            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ringtone = RingtoneManager.getRingtone(applicationContext, uri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) ringtone?.isLooping = true
                ringtone?.play()
            } catch (e: Exception) {}

            try {
                val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
                val compName = ComponentName(this, MyDeviceAdminReceiver::class.java)
                if (dpm.isDeviceOwnerApp(packageName)) { dpm.setLockTaskPackages(compName, arrayOf(packageName)); dpm.setLockTaskFeatures(compName, 0); startLockTask() }
            } catch (e: Exception) {}

            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            btnSubmit.setOnClickListener {
                if (etAnswer.text.toString().trim() == "אני ער לגמרי") finishAlarm()
            }
        } catch (e: Exception) {}
    }

    private fun finishAlarm() {
        ringtone?.stop()
        getSharedPreferences("ShmuelAlarms", Context.MODE_PRIVATE).edit().putBoolean("isAlarmActive", false).apply()
        try { stopLockTask() } catch (e: Exception) {}
        finish()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val g = sqrt(event.values[0]*event.values[0] + event.values[1]*event.values[1] + event.values[2]*event.values[2]) - SensorManager.GRAVITY_EARTH
        if (g > 8.0f) {
            shakes++
            motionProgress.progress = shakes * 2
            if (shakes >= 50) {
                motionProgress.visibility = View.GONE; tvStatus.text = "הקלד כדי להשתחרר:"
                etAnswer.visibility = View.VISIBLE; btnSubmit.visibility = View.VISIBLE
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onResume() { super.onResume(); accel?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) } }
    override fun onPause() { super.onPause(); sensorManager.unregisterListener(this) }
    override fun onDestroy() { super.onDestroy(); ringtone?.stop() }
    override fun onBackPressed() {}
}
