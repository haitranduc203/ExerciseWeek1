package vn.training.bai07
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

open class DemoActivity : AppCompatActivity() {
    lateinit var column: LinearLayout
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24,24,24,24) }
        val scroll = ScrollView(this).apply { addView(column) }
        setContentView(scroll)
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
        }
    }
    fun text(value: String): TextView = TextView(this).apply {
        text = value; textSize = 18f; setPadding(0,12,0,12); column.addView(this)
    }
    fun button(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label; column.addView(this); setOnClickListener { action() }
    }
    fun message(value: String) { Toast.makeText(this, value, Toast.LENGTH_LONG).show() }
}
