package org.tasks.smsgateway

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/** Simple setup screen for the carrier email-to-text feature. Shows as its own launcher icon. */
class SmsGatewaySettingsActivity : Activity() {

    private val fields = LinkedHashMap<String, EditText>()
    private lateinit var enabled: Switch

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Task Texts Setup"
        val p = SmsGateway.prefs(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(32))
        }
        root.addView(TextView(this).apply {
            text = "When a task reminder fires, also send a text through your carrier's " +
                "email-to-SMS gateway. Set up reminders in the Tasks app as usual; use a " +
                "repeating reminder to keep re-texting until you complete the task."
        })

        enabled = Switch(this).apply {
            text = "Send reminder texts"
            isChecked = p.getBoolean(SmsGateway.KEY_ENABLED, false)
        }
        root.addView(enabled)

        fun field(key: String, label: String, default: String = "", type: Int = InputType.TYPE_CLASS_TEXT) {
            root.addView(TextView(this).apply { text = label; setPadding(0, dp(12), 0, 0) })
            val et = EditText(this).apply {
                inputType = type
                setText(p.getString(key, default))
                setSingleLine()
            }
            fields[key] = et
            root.addView(et, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        val email = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        field(SmsGateway.KEY_GATEWAY, "Carrier gateway address (number@carrier-domain)", type = email)
        field(SmsGateway.KEY_HOST, "SMTP server (e.g. smtp.gmail.com)")
        field(SmsGateway.KEY_PORT, "SMTP port (587 or 465)", "587", InputType.TYPE_CLASS_NUMBER)
        field(SmsGateway.KEY_USER, "SMTP username (usually your email)", type = email)
        field(
            SmsGateway.KEY_PASSWORD, "SMTP password (use an app password)",
            type = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        )
        field(SmsGateway.KEY_FROM, "From address (blank = username)", type = email)

        root.addView(Button(this).apply {
            text = "Save"
            setOnClickListener { save(); toast("Saved") }
        })
        root.addView(Button(this).apply {
            text = "Save and send test text"
            setOnClickListener { sendTest() }
        })

        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            addView(root)
        })
    }

    private fun save() {
        val e = SmsGateway.prefs(this).edit()
        e.putBoolean(SmsGateway.KEY_ENABLED, enabled.isChecked)
        fields.forEach { (k, v) -> e.putString(k, v.text.toString()) }
        e.apply()
    }

    private fun sendTest() {
        save()
        toast("Sending...")
        Thread {
            val msg = try {
                SmsGateway.send(this, "Task Texts test: if you got this, delivery works.")
                "Sent. Check your phone."
            } catch (e: Exception) {
                "Failed: ${e.message}"
            }
            runOnUiThread { toast(msg) }
        }.start()
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
