package com.fishbusiness.manager

import android.app.*
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {

    private lateinit var db: Database
    private lateinit var body: LinearLayout

    private val date: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val stamp: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

    private fun money(v: Double) =
        "₹" + String.format(Locale.US, "%.2f", v)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Database(this)
        home()
    }

    private fun page(title: String) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(15, 15, 15, 15)
        }

        val heading = TextView(this).apply {
            text = title
            textSize = 23f
            setTextColor(-1)
            setPadding(15, 20, 15, 20)
            setBackgroundColor(0xFF123047.toInt())
        }
        root.addView(heading)

        val scroll = ScrollView(this)
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(5, 10, 5, 20)
        }

        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(root)
    }

    private fun label(s: String, size: Float = 17f) {
        body.addView(TextView(this).apply {
            text = s
            textSize = size
            setTextColor(0xFF20313A.toInt())
            setPadding(7, 12, 7, 12)
        })
    }

    private fun input(hint: String): EditText {
        val e = EditText(this).apply {
            this.hint = hint
            textSize = 16f
            singleLine = true
        }
        body.addView(e)
        return e
    }

    private fun btn(name: String, action: () -> Unit) {
        body.addView(Button(this).apply {
            text = name
            isAllCaps = false
            setOnClickListener { action() }
        })
    }

    private fun menu() {
        label("━━━━━━━━━━━━━━━━")
        btn("🏠 होम") { home() }
        btn("🧾 नया बिल") { bill() }
        btn("💰 नया खर्च") { expense() }
        btn("👥 ग्राहक") { customers() }
        btn("🚚 वाहन रिकॉर्ड") { vehicles() }
        btn("📊 मासिक रिपोर्ट") { reports() }
    }

    private fun home() {
        page("🐟 Fish Business Manager")

        val sales = db.total("bills", "total", date)
        val expenses = db.total("expenses", "amount", date)

        label("आज की बिक्री", 18f)
        label(money(sales), 25f)
        label("आज का खर्च")
        label(money(expenses), 25f)
        label("आज का अनुमानित मुनाफा")
        label(money(sales - expenses), 25f)

        btn("＋ नया बिल") { bill() }
        btn("＋ नया खर्च") { expense() }
        btn("🚚 वाहन एंट्री") { vehicles() }

        label("हाल के बिल")
        db.rows(
            "SELECT customer,fish,kg,total,date FROM bills ORDER BY id DESC LIMIT 10",
            5
        ).forEach {
            label(it.joinToString(" | "), 13f)
        }

        menu()
    }

    private fun bill() {
        page("नया बिल (GST के बिना)")

        val customer = input("ग्राहक का नाम")
        val phone = input("मोबाइल नंबर")
        val fish = input("मछली का नाम")
        val kg = input("वजन KG")
        val rate = input("रेट प्रति KG")

        val total = TextView(this).apply {
            text = "कुल: ₹0.00"
            textSize = 23f
        }
        body.addView(total)

        val calculate = {
            val w = kg.text.toString().toDoubleOrNull() ?: 0.0
            val r = rate.text.toString().toDoubleOrNull() ?: 0.0
            total.text = "कुल: ${money(w * r)}"
        }

        kg.addTextChangedListener(SimpleWatcher { calculate() })
        rate.addTextChangedListener(SimpleWatcher { calculate() })

        btn("बिल सेव करें") {
            val w = kg.text.toString().toDoubleOrNull()
            val r = rate.text.toString().toDoubleOrNull()

            if (fish.text.isBlank() || w == null || r == null ||
                w <= 0 || r < 0) {
                msg("सही जानकारी भरें")
                return@btn
            }

            db.addBill(
                customer.text.toString(),
                phone.text.toString(),
                fish.text.toString(),
                w, r, date
            )

            AlertDialog.Builder(this)
                .setTitle("बिल सेव हो गया")
                .setMessage(
                    "ग्राहक: ${customer.text}\n" +
                    "मछली: ${fish.text}\n" +
                    "वजन: $w KG\n" +
                    "रेट: ${money(r)}\n" +
                    "कुल: ${money(w * r)}"
                )
                .setPositiveButton("ठीक है") { _, _ -> home() }
                .show()
        }

        menu()
    }

    private fun expense() {
        page("दैनिक खर्च")

        val title = input("खर्च का विवरण")
        val amount = input("रकम ₹")

        btn("खर्च सेव करें") {
            val a = amount.text.toString().toDoubleOrNull()

            if (title.text.isBlank() || a == null || a <= 0) {
                msg("सही जानकारी भरें")
                return@btn
            }

            db.addExpense(title.text.toString(), a, date)
            msg("खर्च सेव हो गया")
            home()
        }

        menu()
    }

    private fun customers() {
        page("ग्राहक रिकॉर्ड")

        val name = input("ग्राहक का नाम")
        val phone = input("मोबाइल नंबर")
        val note = input("पता / जानकारी")

        btn("ग्राहक सेव करें") {
            if (name.text.isBlank()) {
                msg("नाम भरें")
                return@btn
            }

            db.addCustomer(
                name.text.toString(),
                phone.text.toString(),
                note.text.toString()
            )
            msg("ग्राहक सेव हो गया")
            customers()
        }

        label("ग्राहकों की सूची")

        db.rows(
            "SELECT name,phone,note FROM customers ORDER BY id DESC",
            3
        ).forEach {
            label(it.joinToString(" | "), 14f)
        }

        menu()
    }

    private fun vehicles() {
        page("वाहन एंट्री / एग्जिट")

        val number = input("वाहन नंबर")
        val driver = input("ड्राइवर का नाम")
        val phone = input("मोबाइल नंबर")
        val fish = input("मछली / माल")
        val kg = input("लोड वजन KG")

        btn("वाहन एंट्री सेव करें") {
            val w = kg.text.toString().toDoubleOrNull() ?: 0.0

            if (number.text.isBlank()) {
                msg("वाहन नंबर भरें")
                return@btn
            }

            db.addVehicle(
                number.text.toString(),
                driver.text.toString(),
                phone.text.toString(),
                fish.text.toString(),
                w, stamp
            )

            msg("वाहन एंट्री सेव हुई")
            vehicles()
        }

        label("वाहन रिकॉर्ड")

        db.rows(
            "SELECT id,number,driver,fish,kg,entry,exit FROM vehicles ORDER BY id DESC",
            7
        ).forEach { row ->
            label(row.drop(1).joinToString(" | "), 13f)

            if (row[6].isBlank()) {
                btn("${row[1]} का एग्जिट दर्ज करें") {
                    db.vehicleExit(row[0].toLong(), stamp)
                    msg("एग्जिट दर्ज हुई")
                    vehicles()
                }
            }
        }

        menu()
    }

    private fun reports() {
        page("मासिक रिपोर्ट")

        val month = SimpleDateFormat(
            "yyyy-MM", Locale.US
        ).format(Date())

        val sales = db.total("bills", "total", month)
        val expenses = db.total("expenses", "amount", month)

        label("महीना: $month")
        label("कुल बिक्री: ${money(sales)}", 21f)
        label("कुल खर्च: ${money(expenses)}", 21f)
        label("अनुमानित मुनाफा: ${money(sales - expenses)}", 21f)

        menu()
    }

    private fun msg(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }
}
