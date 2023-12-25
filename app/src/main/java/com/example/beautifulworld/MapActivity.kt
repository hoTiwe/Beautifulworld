package com.example.beautifulworld

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.beautifulworld.dto.User
import com.google.android.gms.location.*
import com.google.firebase.database.*
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import androidx.core.app.ActivityCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.squareup.picasso.Picasso
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.runtime.ui_view.ViewProvider
import de.hdodenhof.circleimageview.CircleImageView
import org.w3c.dom.Text
import kotlin.math.abs


class MapActivity : AppCompatActivity() {
    private val apiKey = "47ee5953-0a08-4695-bbe0-c68f95ca93b4"
    var mapView: MapView? = null
    private lateinit var dataBase: DatabaseReference
    private var currentUser: User? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var listUser = arrayListOf<User>()
    private var listPoint = arrayListOf<PlacemarkMapObject>()
    internal lateinit var mLocationRequest: LocationRequest
    private val INTERVAL: Long = 2000
    private val FASTEST_INTERVAL: Long = 1000

    private lateinit var token: String
    override fun onCreate(savedInstanceState: Bundle?) {
        MapKitFactory.setApiKey(apiKey);
        mLocationRequest = LocationRequest()
        val sp = getSharedPreferences("SessionSettings", Context.MODE_PRIVATE)
        token = sp.getString("token", "").toString()
        println("Token in map $token")
        MapKitFactory.initialize(this);
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        dataBase = FirebaseDatabase.getInstance().getReference("User")
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        mapView = findViewById(R.id.mapview)!!;
        getAllUsers()
        startLocationUpdates()
    }

    fun moveCameraToMe(view: View){
        if (currentUser!=null && currentUser!!.location!=null) {
            mapView!!.map.move(
                CameraPosition(currentUser!!.location!!, 12.0f, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 1f),
                null
            )
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun updateMap(){
        listPoint.forEach{mapView!!.map.mapObjects.remove(it)} // удаляем старые точки
        listPoint.clear()
        listUser.forEach{
            if (it.location !=null) {
                val point = mapView!!.map.mapObjects.addPlacemark(
                    Point(
                        it.location!!.latitude, it.location!!.longitude
                    ),
                )
                val view = layoutInflater.inflate(R.layout.point_map, null)
                view.findViewById<TextView>(R.id.name).text = it.name
                val imageView = view.findViewById<CircleImageView>(R.id.icon)
                if (it.linkImage != null) {
                    Picasso.get()
                        .load(it.linkImage)
                        .placeholder(R.drawable.no_icon_user_icon)
                        .error(R.drawable.no_signal_icon)
                        .into(imageView);
                }
                point.setView(ViewProvider(view))
                point.addTapListener { _, _ ->
                    println("Click on point")
//                    val dialog = BottomSheetDialog(this)
//                    val viewBottomSheetDialog =
//                        layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
//                    viewBottomSheetDialog.findViewById<Button>(R.id.botton_close)
//                        .setOnClickListener {
//                            dialog.dismiss()
//                        }
//                    Picasso.get()
//                        .load(it.linkImage)
//                        .placeholder(R.drawable.no_icon_user_icon)
//                        .error(R.drawable.no_icon_user_icon)
//                        .into(viewBottomSheetDialog.findViewById<CircleImageView>(R.id.icon));
//                    viewBottomSheetDialog.findViewById<TextView>(R.id.name).text = it.name
//                    viewBottomSheetDialog.findViewById<TextView>(R.id.number).text = it.phoneNumber
//                    dialog.setCancelable(true)
//                    dialog.setContentView(view)
//                    dialog.show()
                    false
                }
                listPoint.add(point)
            }
        }
    }

    private fun getAllUsers(){
        dataBase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                println("Новое изменение в базе")
                listUser.clear()
                dataSnapshot.children.forEach {
                    it.getValue(User::class.java)?.let { it1 ->
                        listUser.add(it1)
                        println(it.key)
                        if (it.key == token) {
                            println("Это я")
                            if (currentUser==null) {
                                val pos = if (it1.location != null) it1.location!! else Point(
                                        47.211859,
                                        38.924804
                                    )
                                println("Move camera ${pos.latitude} ${pos.longitude}")
                                mapView!!.map.move(
                                    CameraPosition(pos, 12.0f, 0.0f, 0.0f),
                                    Animation(Animation.Type.SMOOTH, 2.5f),
                                    null
                                )
                            }
                            currentUser = it1
                        }
                    }
                }
                updateMap()
            }
            override fun onCancelled(error: DatabaseError) {
                println("Error")
            }
        })
    }

    override fun onStop() {
        mapView!!.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart();
        mapView!!.onStart();
    }

    fun startLocationUpdates() {
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = INTERVAL
        mLocationRequest.fastestInterval = FASTEST_INTERVAL

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest)
        val locationSettingsRequest = builder.build()

        val settingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(locationSettingsRequest)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback,
            Looper.myLooper())
    }
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation
            onLocationChanged(locationResult.lastLocation!!)
        }
    }

    fun onLocationChanged(p0: Location) {
        println("Локация изменилась на: ${p0.latitude} ${p0.longitude}")
        if (currentUser!=null && currentUser!!.location!=null){
            val dLat = abs(currentUser!!.location!!.latitude-p0.latitude)
            val dLon = abs(currentUser!!.location!!.longitude-p0.longitude)
            println("dLat: $dLat dLon: $dLon")
            if (dLat>0.000005f || dLon>0.000005f){
                dataBase.child(token).child("location").setValue(Point(p0.latitude, p0.longitude))
            }
            else {
                println("Изменение не значительно")
            }
        }
        else{
            dataBase.child(token).child("location").setValue(Point(p0.latitude, p0.longitude))
            println("Изменено")
        }
    }
}


