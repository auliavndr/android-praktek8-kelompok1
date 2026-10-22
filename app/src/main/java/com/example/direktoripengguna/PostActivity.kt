package com.example.direktoripengguna

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.direktoripengguna.data.PostBaru
import com.example.direktoripengguna.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class PostActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_USER_NAME = "user_name"
    }

    private var userId = 0

    private lateinit var etJudul: EditText
    private lateinit var btnTambah: Button
    private lateinit var btnCobaLagi: Button
    private lateinit var tvStatus: TextView
    private lateinit var listPost: ListView

    private val daftarJudul = mutableListOf<String>()
    private lateinit var adapterPost: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        userId = intent.getIntExtra(EXTRA_USER_ID, 0)
        title = intent.getStringExtra(EXTRA_USER_NAME) ?: "Postingan"

        etJudul = findViewById(R.id.etJudul)
        btnTambah = findViewById(R.id.btnTambah)
        btnCobaLagi = findViewById(R.id.btnCobaLagi)
        tvStatus = findViewById(R.id.tvStatus)
        listPost = findViewById(R.id.listPost)

        adapterPost = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            daftarJudul
        )

        listPost.adapter = adapterPost

        btnTambah.setOnClickListener {
            kirimPost()
        }

        btnCobaLagi.setOnClickListener {
            muatPosts()
        }

        muatPosts()
    }

    private fun muatPosts() {
        tvStatus.text = "Memuat postingan..."
        btnCobaLagi.visibility = View.GONE
        btnTambah.isEnabled = false

        lifecycleScope.launch {
            try {
                val posts = RetrofitClient.api.getPostsByUser(userId)

                daftarJudul.clear()
                daftarJudul.addAll(posts.map { it.title })
                adapterPost.notifyDataSetChanged()

                tvStatus.text = "${daftarJudul.size} postingan"

            } catch (e: HttpException) {
                tvStatus.text = "Server error: HTTP ${e.code()}"
                btnCobaLagi.visibility = View.VISIBLE

            } catch (e: IOException) {
                tvStatus.text = "Tidak ada koneksi internet"
                btnCobaLagi.visibility = View.VISIBLE

            } finally {
                btnTambah.isEnabled = true
            }
        }
    }

    private fun kirimPost() {
        val judul = etJudul.text.toString().trim()

        if (judul.isEmpty()) {
            etJudul.error = "Judul tidak boleh kosong"
            etJudul.requestFocus()
            return
        }

        if (userId <= 0) {
            tvStatus.text = "ID pengguna tidak valid"
            return
        }

        btnTambah.isEnabled = false
        btnCobaLagi.visibility = View.GONE
        tvStatus.text = "Menyimpan postingan..."

        lifecycleScope.launch {
            try {
                val baru = PostBaru(
                    title = judul,
                    body = "Dibuat dari aplikasi Direktori Pengguna",
                    userId = userId
                )

                val hasil = RetrofitClient.api.buatPost(baru)

                etJudul.text.clear()
                etJudul.error = null

                // Tambahkan postingan ke daftar yang tampil.
                daftarJudul.add(0, hasil.title)
                adapterPost.notifyDataSetChanged()

                tvStatus.text =
                    "Tersimpan dengan id ${hasil.id}"

            } catch (e: HttpException) {
                tvStatus.text =
                    "Gagal menyimpan: HTTP ${e.code()}"
                btnCobaLagi.visibility = View.VISIBLE

            } catch (e: IOException) {
                tvStatus.text = "Tidak ada koneksi internet"
                btnCobaLagi.visibility = View.VISIBLE

            } finally {
                btnTambah.isEnabled = true
            }
        }
    }
}
