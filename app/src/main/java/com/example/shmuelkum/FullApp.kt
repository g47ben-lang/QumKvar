package com.example.shmuelkum

import android.app.*
import android.app.admin.DeviceAdminReceiver
import android.content.*
import android.graphics.Typeface
import android.hardware.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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

class AlarmService : Service() {
    private var ringtone: android.media.Ringtone? = null
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            ringtone = android.media.RingtoneManager.getRingtone(this, uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) ringtone?.isLooping = true
            ringtone?.play()
        } catch (e: Exception) {}
        return START_STICKY
    }
    override fun onDestroy() { super.onDestroy(); ringtone?.stop() }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            context.getSharedPreferences("ShmuelAlarms", Context.MODE_PRIVATE).edit().putBoolean("isAlarmActive", true).apply()
            val alarmIntent = Intent(context, MathAlarmActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) }
            context.startActivity(alarmIntent)
            try { context.startService(Intent(context, AlarmService::class.java)) } catch (e: Exception) {}
        } catch (e: Exception) {}
    }
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!Settings.canDrawOverlays(this)) try { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) } catch (e: Exception) {}

        val rootLayout = findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as? LinearLayout
        val topBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = ViewGroup.LayoutParams(-1, -2); gravity = Gravity.START or Gravity.CENTER_VERTICAL }
        
        val btnSettings = Button(this).apply { text = "⚙️ הגדרות ניהול"; setTextColor(-1); background = null; setOnClickListener { showSettingsDialog() } }
        val btnTest = Button(this).apply { text = "🧪 חלון בדיקה"; setTextColor(-1); background = null; setOnClickListener { showTestDialog() } }
        topBar.addView(btnSettings); topBar.addView(btnTest)
        rootLayout?.addView(topBar, 0)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        viewPager.adapter = PagerAdapter()
        TabLayoutMediator(tabLayout, viewPager) { tab, pos -> tab.text = when(pos) { 0 -> "השכמה"; 1 -> "שנ\"צ"; else -> "שבועי" } }.attach()
    }

    override fun onResume() {
        super.onResume()
        if (getSharedPreferences("ShmuelAlarms", Context.MODE_PRIVATE).getBoolean("isAlarmActive", false)) {
            startActivity(Intent(this, MathAlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
    }

    private fun showTestDialog() {
        AlertDialog.Builder(this).setTitle("מצב סימולטור (בדיקה)").setMessage("איך תרצה לבדוק את האזעקה?\n\n• 'מצב אמת': בודק בדיוק את מה ששמור במערכת כרגע.\n• 'בדיקה מותאמת': מאפשר לבחור קושי רק לצורך הבדיקה.")
            .setPositiveButton("מצב אמת") { _, _ -> startActivity(Intent(this, MathAlarmActivity::class.java).putExtra("TEST_MODE", true)) }
            .setNeutralButton("בדיקה מותאמת") { _, _ -> showCustomTestDialog() }.setNegativeButton("ביטול", null).show()
    }

    private fun showCustomTestDialog() {
        val prefs = getSharedPreferences("ShmuelAlarms", Context.MODE_PRIVATE)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 50, 50, 50); gravity = Gravity.CENTER }
        val tvDiff = TextView(this).apply { text = "בחר רמת קושי לבדיקה"; textSize = 20f; gravity = Gravity.CENTER; setPadding(0, 0, 0, 20) }
        val scrollGroup = HorizontalScrollView(this)
        val rg = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL; gravity = Gravity.CENTER }
        val rbIds = IntArray(5) 
        for (i in 1..5) { val rb = RadioButton(this).apply { text = "$i"; id = View.generateViewId(); textSize = 18f; setPadding(20,10,20,20) }; rbIds[i-1] = rb.id; rg.addView(rb) }
        rg.check(rbIds[prefs.getInt("difficulty", 2) - 1]); scrollGroup.addView(rg)
        val cbTyping = CheckBox(this).apply { text = "דרוש הקלדה"; isChecked = prefs.getBoolean("task_typing", true); textSize = 16f }
        val cbLevel = CheckBox(this).apply { text = "דרוש פלס"; isChecked = prefs.getBoolean("task_level", true); textSize = 16f }
        layout.addView(tvDiff); layout.addView(scrollGroup); layout.addView(cbTyping); layout.addView(cbLevel)

        AlertDialog.Builder(this).setTitle("בדיקה מותאמת").setView(layout).setPositiveButton("התחל בדיקה") { _, _ ->
            val idx = rbIds.indexOf(rg.checkedRadioButtonId)
            prefs.edit().putInt("difficulty", if (idx != -1) idx + 1 else 2).putBoolean("task_typing", cbTyping.isChecked).putBoolean("task_level", cbLevel.isChecked).apply()
            startActivity(Intent(this, MathAlarmActivity::class.java).putExtra("TEST_MODE", true))
        }.show()
    }

    private fun showSettingsDialog() {
        val options = arrayOf("👑 רוט מלא (מומלץ: ניהול + חסימת כיבוי)", "🛡️ רוט חלקי (רק פקודת ADB למנהל)", "🔓 בטל הרשאות (שחרר ניהול)", "💻 הוראות ADB ללא רוט")
        AlertDialog.Builder(this).setTitle("הגדרות הרשאות וניהול").setItems(options) { _, which ->
            val prefs = getSharedPreferences("ShmuelAlarms", Context.MODE_PRIVATE)
            when (which) {
                0 -> { prefs.edit().putBoolean("use_root_daemon", true).apply(); executeRootCommand("dpm set-device-owner com.example.shmuelkum/.MyDeviceAdminReceiver && appops set com.example.shmuelkum SYSTEM_ALERT_WINDOW allow && pm grant com.example.shmuelkum android.permission.WRITE_SECURE_SETTINGS") }
                1 -> { prefs.edit().putBoolean("use_root_daemon", false).apply(); executeRootCommand("dpm set-device-owner com.example.shmuelkum/.MyDeviceAdminReceiver") }
                2 -> { try { val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager; dpm.clearDeviceOwnerApp(packageName); Toast.makeText(this, "הניהול שוחרר", Toast.LENGTH_SHORT).show() } catch (e: Exception) {} }
                3 -> { AlertDialog.Builder(this).setTitle("פקודת ADB").setMessage("adb shell dpm set-device-owner com.example.shmuelkum/.MyDeviceAdminReceiver").setPositiveButton("הבנתי", null).show() }
            }
        }.show()
    }

    private fun executeRootCommand(command: String) {
        Toast.makeText(this, "מבקש הרשאות רוט...", Toast.LENGTH_SHORT).show()
        Thread { try { val p = Runtime.getRuntime().exec(arrayOf("su", "-c", command)); p.waitFor() } catch (e: Exception) {} }.start()
    }

    inner class PagerAdapter : RecyclerView.Adapter<PagerAdapter.PageViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder = PageViewHolder(LinearLayout(parent.context).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 40, 50, 40); layoutParams = ViewGroup.LayoutParams(-1, -1); gravity = Gravity.CENTER_HORIZONTAL })
        override fun onBindViewHolder(holder: PageViewHolder, pos: Int) {
            holder.container.removeAllViews()
            when(pos) { 0 -> setupFixedPage(holder.container); 1 -> setupQuickPage(holder.container); 2 -> setupWeeklyPage(holder.container) }
        }
        override fun getItemCount() = 3
        inner class PageViewHolder(val container: LinearLayout) : RecyclerView.ViewHolder(container)
    }

    private fun setupFixedPage(container: LinearLayout) {
        val tp = TimePicker(ContextThemeWrapper(this, R.style.BlueClockStyle)).apply { setIs24HourView(true); background = null }
        val btn = createGhostButton("קבע השכמה") {
            val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, tp.hour); set(Calendar.MINUTE, tp.minute); set(Calendar.SECOND, 0) }
            if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
            showDifficultyStep(container, cal)
        }
        container.addView(tp); container.addView(btn)
    }

    private fun setupQuickPage(container: LinearLayout) {
        val et = EditText(this).apply { hint = "00"; inputType = 2; textSize = 90f; setTextColor(-1); gravity = Gravity.CENTER; background = null }
        val btn = createGhostButton("התחל ספירה לאחור לשנ\"צ") {
            val m = et.text.toString(); if (m.isNotEmpty()) showDifficultyStep(container, Calendar.getInstance().apply { add(Calendar.MINUTE, m.toInt()) })
        }
        container.addView(et); container.addView(btn)
    }

    private fun setupWeeklyPage(container: LinearLayout) {
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val days = listOf("ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת")
        val prefs = getSharedPreferences("ShmuelAlarms", Context.MODE_PRIVATE)
        for (i in 1..7) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(30, 15, 30, 15); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 8, 0, 8) } }
            val tv = TextView(this).apply { text = days[i-1]; textSize = 16f; setTextColor(-1); layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
            val sw = Switch(this).apply { isChecked = prefs.getBoolean("d_${i}_a", false); setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean("d_${i}_a", v).apply(); scheduleNextWeeklyAlarm(this@MainActivity) } }
            val btnTime = Button(this).apply {
                text = String.format("%02d:%02d", prefs.getInt("d_${i}_h", 7), prefs.getInt("d_${i}_m", 0)); background = null; setTextColor(0xFFBBDEFB.toInt())
                setOnClickListener {
                    TimePickerDialog(this@MainActivity, R.style.BlueClockStyle, { _, h, m -> prefs.edit().putInt("d_${i}_h", h).putInt("d_${i}_m", m).putBoolean("d_${i}_a", true).apply(); text = String.format("%02d:%02d", h, m); sw.isChecked = true; scheduleNextWeeklyAlarm(this@MainActivity) }, prefs.getInt("d_${i}_h", 7), prefs.getInt("d_${i}_m", 0), true).show()
                }
            }
            row.addView(tv); row.addView(btnTime); row.addView(sw); list.addView(row)
        }
        scroll.addView(list); container.addView(scroll)
    }

    private fun scheduleNextWeeklyAlarm(context: Context) {
        val prefs = context.getSharedPreferences("ShmuelAlarms", Context.MODE_PRIVATE)
        var minDelay = Long.MAX_VALUE; var nextAlarmCal: Calendar? = null
        for (i in 1..7) {
            if (prefs.getBoolean("d_${i}_a", false)) {
                val checkCal = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, i); set(Calendar.HOUR_OF_DAY, prefs.getInt("d_${i}_h", 7)); set(Calendar.MINUTE, prefs.getInt("d_${i}_m", 0)); set(Calendar.SECOND, 0) }
                if (checkCal.timeInMillis <= System.currentTimeMillis()) checkCal.add(Calendar.WEEK_OF_YEAR, 1)
                val delay = checkCal.timeInMillis - System.currentTimeMillis()
                if (delay < minDelay) { minDelay = delay; nextAlarmCal = checkCal }
            }
        }
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(context, 9999, Intent(context, AlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val showPi = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (nextAlarmCal != null) { am.setAlarmClock(AlarmManager.AlarmClockInfo(nextAlarmCal.timeInMillis, showPi), pi); Toast.makeText(context, "לו\"ז שבועי: מעודכן", Toast.LENGTH_SHORT).show() } else am.cancel(pi)
    }

    private fun showDifficultyStep(container: LinearLayout, cal: Calendar) {
        container.removeAllViews(); container.gravity = Gravity.CENTER
        val prefs = getSharedPreferences("ShmuelAlarms", Context.MODE_PRIVATE)
        val tvDiff = TextView(this).apply { text = "בחר את רמת הסבל"; textSize = 22f; setTextColor(-1) }
        val scrollGroup = HorizontalScrollView(this)
        val rg = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL; gravity = Gravity.CENTER }
        val rbIds = IntArray(5) 
        for (i in 1..5) { val rb = RadioButton(this).apply { text = "$i"; id = View.generateViewId(); setTextColor(-1); textSize = 20f; setPadding(20,10,20,40) }; rbIds[i-1] = rb.id; rg.addView(rb) }
        rg.check(rbIds[prefs.getInt("difficulty", 2) - 1]); scrollGroup.addView(rg)
        val cbTyping = CheckBox(this).apply { text = "דרוש משימת הקלדה"; setTextColor(-1); isChecked = prefs.getBoolean("task_typing", true) }
        val cbLevel = CheckBox(this).apply { text = "דרוש משימת פלס"; setTextColor(-1); isChecked = prefs.getBoolean("task_level", true) }
        
        val btnOk = createGhostButton("הפעל כלא סופית") {
            prefs.edit().putInt("difficulty", if (rbIds.indexOf(rg.checkedRadioButtonId) != -1) rbIds.indexOf(rg.checkedRadioButtonId) + 1 else 2).putBoolean("task_typing", cbTyping.isChecked).putBoolean("task_level", cbLevel.isChecked).apply()
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(this, (cal.timeInMillis % 10000).toInt(), Intent(this, AlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val showPi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            am.setAlarmClock(AlarmManager.AlarmClockInfo(cal.timeInMillis, showPi), pi)
            showWaitScreen(container)
        }
        container.addView(tvDiff); container.addView(scrollGroup); container.addView(cbTyping); container.addView(cbLevel); container.addView(btnOk)
    }

    private fun showWaitScreen(container: LinearLayout) {
        container.removeAllViews(); container.setPadding(0,0,0,0)
        findViewById<View>(R.id.tabLayout).visibility = View.GONE
        findViewById<View>(android.R.id.content).setBackgroundColor(0xFF000000.toInt())
        val tv = TextView(this).apply { text = "אני אדאג שתלמד לקום בזמן!"; textSize = 45f; setTypeface(null, Typeface.BOLD); setTextColor(-1); gravity = Gravity.CENTER; setShadowLayer(35f, 0f, 0f, 0xFF000000.toInt()); layoutParams = LinearLayout.LayoutParams(-1, -1).apply { gravity = Gravity.CENTER } }
        container.addView(tv)
    }

    private fun createGhostButton(txt: String, onClick: () -> Unit) = Button(this).apply { text = txt; setTextColor(-1); textSize = 18f; background = android.graphics.drawable.GradientDrawable().apply { setStroke(3, -1); cornerRadius = 25f; setColor(0x00000000) }; layoutParams = LinearLayout.LayoutParams(650, 140).apply { setMargins(0, 40, 0, 0) }; setOnClickListener { onClick() } }
}

class MathAlarmActivity : AppCompatActivity(), SensorEventListener {
    private var step = 0; private var shakes = 0; private var stillTime: Long = 0L; private var difficulty = 2; private var reqTyping = true; private var reqLevel = true; private lateinit var sensorManager: SensorManager; private var accel: Sensor?; @Volatile private var isTaskRunning = true
    init { accel = null }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) { setShowWhenLocked(true); setTurnScreenOn(true) } else { window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON) }
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_FULLSCREEN)
            setContentView(R.layout.activity_math_alarm)
            val rootLayout = findViewById<View>(android.R.id.content) as ViewGroup

            val quotes = listOf("מקקים נשארים במיטה, בחורים רציניים קמים לתפילה!", "המיטה שואבת מקקים, תראה לה שאתה אריה!", "הסטייפלר לא נהיה גדול מלישון עד 9. קום!", "זמן קריאת שמע לא מתחשב בעייפות שלך. עכשיו!", "התירוצים של אתמול לא יעזרו לך מול המשגיח. קום כבר!")
            val tvMotivation = TextView(this).apply { text = "\"${quotes.random()}\""; textSize = 24f; setTextColor(0xFFFFD700.toInt()); gravity = Gravity.CENTER; setShadowLayer(10f, 0f, 0f, 0xFF000000.toInt()); layoutParams = FrameLayout.LayoutParams(-1, -2).apply { gravity = Gravity.TOP; setMargins(30, 100, 30, 0) } }
            rootLayout.addView(tvMotivation)

            if (intent.getBooleanExtra("TEST_MODE", false)) {
                val btnEmergency = Button(this).apply { text = "🛑 כפתור גיבוי (יציאת חירום מבדיקה)"; setBackgroundColor(0xCCFF0000.toInt()); setTextColor(-1); layoutParams = FrameLayout.LayoutParams(-2, -2).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; setMargins(0, 0, 0, 150) }; setOnClickListener { finishAlarm() } }
                rootLayout.addView(btnEmergency)
            }

            try { val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager; val compName = ComponentName(this, MyDeviceAdminReceiver::class.java); if (dpm.isDeviceOwnerApp(packageName)) { dpm.setLockTaskPackages(compName, arrayOf(packageName)); dpm.setLockTaskFeatures(compName, 0); startLockTask() } } catch (e: Exception) {}

            val prefs = getSharedPreferences("ShmuelAlarms", Context.MODE_PRIVATE)
            difficulty = prefs.getInt("difficulty", 2); reqTyping = prefs.getBoolean("task_typing", true); reqLevel = prefs.getBoolean("task_level", true)

            if (prefs.getBoolean("use_root_daemon", false)) Thread { try { val process = Runtime.getRuntime().exec("su"); val os = DataOutputStream(process.outputStream); while (isTaskRunning) { if (step != 1) { os.writeBytes("input keyevent 4\n"); os.flush() }; Thread.sleep(400) }; os.writeBytes("exit\n"); os.flush(); process.waitFor() } catch (e: Exception) {} }.start()

            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accel == null) { reqLevel = false; Handler(Looper.getMainLooper()).postDelayed({ moveToNextStep(0) }, 1000) }

            val etAnswer = findViewById<EditText>(R.id.etAnswer); val btnSubmit = findViewById<Button>(R.id.btnSubmit)
            btnSubmit.setOnClickListener { if (etAnswer.text.toString().trim() == "אני ער לגמרי") { (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(etAnswer.windowToken, 0); moveToNextStep(1) } }
        } catch (e: Exception) {}
    }

    private fun moveToNextStep(currentStep: Int) {
        val etAnswer = findViewById<EditText>(R.id.etAnswer); val btnSubmit = findViewById<Button>(R.id.btnSubmit); val motionProgress = findViewById<ProgressBar>(R.id.motionProgress); val tvStatus = findViewById<TextView>(R.id.tvStatus)
        if (currentStep == 0) { if (reqTyping) { step = 1; tvStatus.text = "הקלד: אני ער לגמרי"; motionProgress.visibility = View.GONE; etAnswer.visibility = View.VISIBLE; btnSubmit.visibility = View.VISIBLE; etAnswer.requestFocus() } else if (reqLevel) { step = 2; stillTime = 0L; motionProgress.visibility = View.VISIBLE; tvStatus.text = "פלס: החזק ישר!" } else finishAlarm() }
        else if (currentStep == 1) { etAnswer.visibility = View.GONE; btnSubmit.visibility = View.GONE; if (reqLevel) { step = 2; stillTime = 0L; motionProgress.visibility = View.VISIBLE; tvStatus.text = "פלס: החזק ישר!" } else finishAlarm() }
    }

    private fun finishAlarm() {
        step = 3; isTaskRunning = false
        getSharedPreferences("ShmuelAlarms", Context.MODE_PRIVATE).edit().putBoolean("isAlarmActive", false).apply()
        stopService(Intent(this, AlarmService::class.java))
        try { stopLockTask() } catch (e: Exception) {}
        finish()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if (!hasFocus && isTaskRunning) sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean { if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) return true; return super.onKeyDown(keyCode, event) }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || accel == null) return
        try {
            val x = event.values[0]; val y = event.values[1]; val z = event.values[2]; val g = sqrt(x*x + y*y + z*z) - SensorManager.GRAVITY_EARTH
            val motionProgress = findViewById<ProgressBar>(R.id.motionProgress)
            val reqShakes = difficulty * 20; val gThreshold = 3.0f + (difficulty * 2.5f); val reqStillSec = 2 + (difficulty * 2)

            if (step == 0) { if (g > gThreshold) { shakes++; motionProgress.progress = (shakes * 100 / reqShakes); if (shakes >= reqShakes) moveToNextStep(0) } }
            else if (step == 2) { val tol = 3.0f - (difficulty * 0.4f); if (abs(x) < tol && abs(y) > (9.8f - tol)) { if (stillTime == 0L) stillTime = System.currentTimeMillis(); val sec = (System.currentTimeMillis() - stillTime) / 1000; motionProgress.progress = (sec.toInt() * 100 / reqStillSec); if (sec >= reqStillSec) finishAlarm() } else { stillTime = 0L; motionProgress.progress = 0 } }
        } catch (e: Exception) {}
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onResume() { super.onResume(); accel?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) } }
    override fun onPause() { super.onPause(); sensorManager.unregisterListener(this) }
    override fun onDestroy() { super.onDestroy(); isTaskRunning = false }
    override fun onBackPressed() {} 
}
