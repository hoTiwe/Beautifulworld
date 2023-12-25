package com.example.beautifulworld

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.beautifulworld.dto.User
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.yandex.mapkit.geometry.Point
import de.hdodenhof.circleimageview.CircleImageView
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import id.zelory.compressor.constraint.size
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream


class MainActivity : AppCompatActivity() {
    private lateinit var user: User
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var dataBase: DatabaseReference
    private lateinit var sp: SharedPreferences
    private var selectedImage: Uri? = null
    private var storageRef = Firebase.storage
    private lateinit var mProgressDialog: ProgressDialog
    val context = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sp = getSharedPreferences("SessionSettings", Context.MODE_PRIVATE)
        val token = sp.getString("token", null)
        if (token != null) {
            println("У меня уже есть токен: $token")
            startActivity(Intent(this, MapActivity::class.java))
        }
        else{
            println("У меня нет токена((")
        }

        setContentView(R.layout.activity_main)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        user = User()
        dataBase = FirebaseDatabase.getInstance().getReference("User")
        val f1 = FragmentRegistration1()
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, f1)
        ft.commit()
    }

    fun selectImage(view:View){
        val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(gallery, 100)
    }

    fun getPermission(view: View){
        if (selectedImage!=null) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                println("Get permission")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    101
                )
            } else {
                uploadImage(view)
            }
        }
        else{
            goToMap(view)
        }
    }

    fun uploadImage(view: View){
        if (selectedImage!=null) {
            if (ActivityCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
                println("Get permission")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 101)
            }

            mProgressDialog = ProgressDialog(this)
            mProgressDialog.setMessage("Пару секунд...")
            mProgressDialog.show()


            println("Делаем файл $selectedImage")
            val uriPathHelper = URIPathHelper()
            val filePath = uriPathHelper.getPath(context, selectedImage!!)

            var file = File(filePath)
            GlobalScope.launch {
                println("получаем размер")
                val size = FileInputStream(file).channel.size()

                if (size > 256 * 1024) {
                    println("Фото большое $size")
                    try {


                        file = Compressor.compress(context = context, imageFile = file) {
                            resolution(720, 720)
                            quality(40)
                            format(Bitmap.CompressFormat.WEBP)
                            size(262_144) // 256 KB
                        }

                        println("Сжатый: ${FileInputStream(file).channel.size()}")
                    } catch (e: Exception) {
                        throw Exception("Big size")
                    }
                }
                storageRef.getReference("users").child(System.currentTimeMillis().toString())
                    .putFile(selectedImage!!).addOnSuccessListener { task ->
                        task.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                            user.linkImage = it.toString()
                            println("URL : $it")
                            mProgressDialog.dismiss()
                            goToMap(view)
                        }
                    }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 100) {
            selectedImage = data?.data
            val civ = findViewById<CircleImageView>(R.id.selectedImage)
            civ.setImageURI(selectedImage)
            civ.visibility = View.VISIBLE
            findViewById<Button>(R.id.upload).text = "Загрузить"
        }
    }

    fun takeLocation(view: View){
        val task = fusedLocationProviderClient.lastLocation
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
        }
        task.addOnSuccessListener {
            if (it!=null){
                user.location = Point(it.latitude, it.longitude)
                dataBase.push().setValue(user)
                println("Создем пользователя: ${user.linkImage} ${user.phoneNumber}")
                val context = this
                dataBase.orderByChild("phoneNumber")
                    .equalTo(user.phoneNumber)
                    .addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onCancelled(error: DatabaseError) {
                                println(error.message)
                            }
                            override fun onDataChange(snapshot: DataSnapshot) {
                                var key: String? = null
                                snapshot.children.forEach{ key = it.key}
                                sp.edit().putString("token", key).commit()
                                println("New token: ${sp.getString("token", "")}")
                                startActivity(Intent(context, MapActivity::class.java))
                            }
                        })
            }
        }
    }

    fun goToSecondFragment(view: View){
        user.name = findViewById<EditText>(R.id.inputName).text.toString()
        val f1 = FragmentRegistration2()
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, f1)
        ft.commit()
    }
    fun goToThirdFragment(view: View){
        user.phoneNumber = findViewById<EditText>(R.id.inputPhoneNumber).text.toString()
        val f1 = FragmentRegistration3()
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, f1)
        ft.commit()
    }

    fun goToMap(view: View){
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            val f1 = FragmentLocation()
            val ft = supportFragmentManager.beginTransaction()
            ft.replace(R.id.fragment_container, f1)
            ft.commit()
        }
        else{
            takeLocation(View(this))
        }
    }
}