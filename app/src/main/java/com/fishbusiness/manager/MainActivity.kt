package com.fishbusiness.manager

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var db: Database
    private lateinit var body: LinearLayout

    private val date: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .format(Date())

    private val stamp: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            .format(Date())

    private fun money(value: Double): String {
        return "₹" + String.format(Locale.US, "%.2f", value)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Database(this)
        home()
    }

    private fun page(title: String) {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(15, 15, 15, 15)
            setBackgroundColor(0xFFF4F7F8.toInt())
        }

        val heading = TextView(this).apply {
            text = title
            textSize = 23f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(15, 20, 15, 20)
            setBackgroundColor(0xFF123047.toInt())
        }

        root.addView(heading)

        val scroll = ScrollView(this)

        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 12, 8, 20)
        }

        scroll.addView(body)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun label(
        value: String,
        size: Float = 17f
    ) {
        body.addView(TextView(this).apply {
            text = value
            textSize = size
            setTextColor(0xFF20313A.toInt())
            setPadding(7, 12, 7, 12)
        })
    }

    private fun input(hintText: String): EditText {

        val edit = EditText(this).apply {
            hint = hintText
            textSize = 16f
            setSingleLine(true)
            setPadding(12, 8, 12, 8)
        }

        body.addView(
            edit,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        return edit
    }

    private fun btn(
        title: String,
        action: () -> Unit
    ) {
        body.addView(Button(this).apply {
            text = title
            isAllCaps = false
            setOnClickListener {
                action()
            }
        })
    }

    private fun menu() {

        label("━━━━━━━━━━━━━━━━")

        btn("🏠 होम") {
            home()
        }

        btn("🧾 नया बिल") {
            bill()
        }

        btn("💰 नया खर्च") {
            expense()
        }

        btn("👥 ग्राहक रिकॉर्ड") {
            customers()
        }

        btn("🚚 वाहन रिकॉर्ड") {
            vehicles()
        }

        btn("📊 मासिक रिपोर्ट") {
            reports()
        }
    }

    // HOME DASHBOARD

    private fun home() {

        page("🐟 Fish Business Manager")

        val sales = db.total(
            "bills",
            "total",
            date
        )

        val expenses = db.total(
            "expenses",
            "amount",
            date
        )

        label("आज की बिक्री", 18f)
        label(money(sales), 25f)

        label("आज का खर्च", 18f)
        label(money(expenses), 25f)

        label("आज का अनुमानित मुनाफा", 18f)
        label(money(sales - expenses), 25f)

        btn("＋ नया बिल") {
            bill()
        }

        btn("＋ नया खर्च") {
            expense()
        }

        btn("🚚 वाहन एंट्री") {
            vehicles()
        }

        label("हाल के बिल")

        db.rows(
            "SELECT customer,fish,kg,total,date FROM bills ORDER BY id DESC LIMIT 10",
            5
        ).forEach {
            label(it.joinToString(" | "), 13f)
        }

        menu()
    }

    // BILLING

    private fun bill() {

        page("नया बिल - GST के बिना")

        val customer = input("ग्राहक का नाम")
        val phone = input("मोबाइल नंबर")
        val fish = input("मछली का नाम")
        val kg = input("वजन किलो में")
        val rate = input("रेट प्रति किलो ₹")

        val total = TextView(this).apply {
            text = "कुल रकम: ₹0.00"
            textSize = 23f
            setTextColor(0xFF087E8B.toInt())
            setPadding(8, 15, 8, 15)
        }

        body.addView(total)

        val calculate = {

            val weight =
                kg.text.toString().toDoubleOrNull() ?: 0.0

            val price =
                rate.text.toString().toDoubleOrNull() ?: 0.0

            total.text = "कुल रकम: ${money(weight * price)}"
        }

        kg.addTextChangedListener(
            SimpleWatcher { calculate() }
        )

        rate.addTextChangedListener(
            SimpleWatcher { calculate() }
        )

        btn("बिल सेव करें") {

            val weight =
                kg.text.toString().toDoubleOrNull()

            val price =
                rate.text.toString().toDoubleOrNull()

            if (
                fish.text.isBlank() ||
                weight == null ||
                price == null ||
                weight <= 0 ||
                price < 0
            ) {
                message("सही जानकारी भरें")
                return@btn
            }

            db.addBill(
                customer.text.toString(),
                phone.text.toString(),
                fish.text.toString(),
                weight,
                price,
                date
            )

            AlertDialog.Builder(this)
                .setTitle("बिल सेव हो गया")
                .setMessage(
                    "ग्राहक: ${customer.text}\n" +
                    "मोबाइल: ${phone.text}\n" +
                    "मछली: ${fish.text}\n" +
                    "वजन: $weight KG\n" +
                    "रेट: ${money(price)}\n" +
                    "कुल: ${money(weight * price)}\n" +
                    "दिनांक: $date"
                )
                .setPositiveButton("ठीक है") { _, _ ->
                    home()
                }
                .show()
        }

        menu()
    }

    // EXPENSE

    private fun expense() {

        page("दैनिक खर्च")

        val title = input("खर्च का विवरण")
        val amount = input("रकम ₹")

        btn("खर्च सेव करें") {

            val value =
                amount.text.toString().toDoubleOrNull()

            if (
                title.text.isBlank() ||
                value == null ||
                value <= 0
            ) {
                message("सही जानकारी भरें")
                return@btn
            }

            db.addExpense(
                title.text.toString(),
                value,
                date
            )

            message("खर्च सेव हो गया")
            home()
        }

        menu()
    }

    // CUSTOMERS

    private fun customers() {

        page("ग्राहक रिकॉर्ड")

        val name = input("ग्राहक का नाम")
        val phone = input("मोबाइल नंबर")
        val note = input("पता / जानकारी")

        btn("ग्राहक सेव करें") {

            if (name.text.isBlank()) {
                message("ग्राहक का नाम भरें")
                return@btn
            }

            db.addCustomer(
                name.text.toString(),
                phone.text.toString(),
                note.text.toString()
            )

            message("ग्राहक सेव हो गया")
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

    // VEHICLE ENTRY AND EXIT

    private fun vehicles() {

        page("वाहन एंट्री / एग्जिट")

        val number = input("वाहन नंबर")
        val driver = input("ड्राइवर का नाम")
        val phone = input("मोबाइल नंबर")
        val fish = input("मछली / माल")
        val kg = input("लोड वजन KG")

        btn("वाहन एंट्री सेव करें") {

            val weight =
                kg.text.toString().toDoubleOrNull() ?: 0.0

            if (number.text.isBlank()) {
                message("वाहन नंबर भरें")
                return@btn
            }

            db.addVehicle(
                number.text.toString(),
                driver.text.toString(),
                phone.text.toString(),
                fish.text.toString(),
                weight,
                stamp
            )

            message("वाहन एंट्री सेव हो गई")
            vehicles()
        }

        label("वाहन रिकॉर्ड")

        db.rows(
            "SELECT id,number,driver,fish,kg,entry,exit FROM vehicles ORDER BY id DESC",
            7
        ).forEach { row ->

            label(
                "वाहन: ${row[1]}\n" +
                "ड्राइवर: ${row[2]}\n" +
                "माल: ${row[3]}\n" +
                "वजन: ${row[4]} KG\n" +
                "एंट्री: ${row[5]}\n" +
                "एग्जिट: ${if (row[6].isBlank()) "बाकी है" else row[6]}",
                13f
            )

            if (row[6].isBlank()) {

                btn("${row[1]} - एग्जिट दर्ज करें") {

                    db.vehicleExit(
                        row[0].toLong(),
                        stamp
                    )

                    message("एग्जिट दर्ज हो गई")
                    vehicles()
                }
            }
        }

        menu()
    }

    // MONTHLY REPORT

    private fun reports() {

        page("मासिक रिपोर्ट")

        val month = SimpleDateFormat(
            "yyyy-MM",
            Locale.US
        ).format(Date())

        val sales = db.total(
            "bills",
            "total",
            month
        )

        val expenses = db.total(
            "expenses",
            "amount",
            month
        )

        label("महीना: $month", 18f)

        label(
            "कुल बिक्री: ${money(sales)}",
            21f
        )

        label(
            "कुल खर्च: ${money(expenses)}",
            21f
        )

        label(
            "अनुमानित मुनाफा: ${money(sales - expenses)}",
            21f
        )

        label(
            "नोट: मुनाफा दर्ज बिक्री में से दर्ज खर्च घटाकर निकाला गया है।",
            13f
        )

        menu()
    }

    private fun message(text: String) {

        Toast.makeText(
            this,
            text,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }
}
