package com.fishbusiness.manager

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class Database(context: Context) :
    SQLiteOpenHelper(context, "fish_manager.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE bills(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                customer TEXT,
                phone TEXT,
                fish TEXT,
                kg REAL,
                rate REAL,
                total REAL,
                date TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE expenses(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT,
                amount REAL,
                date TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE customers(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                phone TEXT,
                note TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE vehicles(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                number TEXT,
                driver TEXT,
                phone TEXT,
                fish TEXT,
                kg REAL,
                entry TEXT,
                exit TEXT
            )
        """)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {}

    fun addBill(
        customer: String, phone: String,
        fish: String, kg: Double,
        rate: Double, date: String
    ) {
        val v = ContentValues().apply {
            put("customer", customer)
            put("phone", phone)
            put("fish", fish)
            put("kg", kg)
            put("rate", rate)
            put("total", kg * rate)
            put("date", date)
        }
        writableDatabase.insert("bills", null, v)
    }

    fun addExpense(title: String, amount: Double, date: String) {
        val v = ContentValues().apply {
            put("title", title)
            put("amount", amount)
            put("date", date)
        }
        writableDatabase.insert("expenses", null, v)
    }

    fun addCustomer(name: String, phone: String, note: String) {
        val v = ContentValues().apply {
            put("name", name)
            put("phone", phone)
            put("note", note)
        }
        writableDatabase.insert("customers", null, v)
    }

    fun addVehicle(
        number: String, driver: String,
        phone: String, fish: String,
        kg: Double, entry: String
    ) {
        val v = ContentValues().apply {
            put("number", number)
            put("driver", driver)
            put("phone", phone)
            put("fish", fish)
            put("kg", kg)
            put("entry", entry)
            put("exit", "")
        }
        writableDatabase.insert("vehicles", null, v)
    }

    fun vehicleExit(id: Long, time: String) {
        val v = ContentValues().apply { put("exit", time) }
        writableDatabase.update(
            "vehicles", v, "id=?",
            arrayOf(id.toString())
        )
    }

    fun total(table: String, column: String, month: String): Double {
        val c = readableDatabase.rawQuery(
            "SELECT COALESCE(SUM($column),0) FROM $table WHERE date LIKE ?",
            arrayOf("$month%")
        )
        c.use {
            return if (it.moveToFirst()) it.getDouble(0) else 0.0
        }
    }

    fun rows(sql: String, count: Int): List<List<String>> {
        val result = mutableListOf<List<String>>()
        readableDatabase.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) {
                result.add((0 until count).map {
                    c.getString(it) ?: ""
                })
            }
        }
        return result
    }
    }
