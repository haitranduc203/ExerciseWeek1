package vn.training.bai02

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.widget.doAfterTextChanged
class MainActivity : DemoActivity() {
    private lateinit var nameInput: EditText
    private lateinit var greeting: TextView
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val prefs=getSharedPreferences("input",MODE_PRIVATE)
        text(getString(R.string.title))
        nameInput=EditText(this).apply { id=R.id.name_input; hint=getString(R.string.name_hint); setText(state?.getString("name") ?: prefs.getString("name","Android")); column.addView(this) }
        greeting=text("")
        fun update() { greeting.text=getString(R.string.greeting,nameInput.text.toString()) }
        nameInput.doAfterTextChanged { update(); prefs.edit().putString("name",it.toString()).apply() }; update()
        listOf(0,1,2,5).forEach { count -> text(if(count==0) getString(R.string.zero) else resources.getQuantityString(R.plurals.messages,count,count)) }
        listOf("English" to "en","Tiếng Việt" to "vi","日本語" to "ja",getString(R.string.system) to "").forEach { (label,tag) ->
            button(label) { prefs.edit().putString("name",nameInput.text.toString()).apply(); AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag)) }
        }
        text(getString(R.string.fallback))
    }
    override fun onSaveInstanceState(out: Bundle) { out.putString("name",nameInput.text.toString()); super.onSaveInstanceState(out) }
}
