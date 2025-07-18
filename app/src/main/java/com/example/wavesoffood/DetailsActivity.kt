package com.example.wavesoffood

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.wavesoffood.databinding.ActivityDetailsBinding
import com.example.wavesoffood.model.CartItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding
    private lateinit var auth: FirebaseAuth

    private var foodName: String? = null
    private var foodImage: String? = null
    private var foodDescription: String? = null
    private var foodIngredients: String? = null
    private var foodPrice: String? = null
    private var hotelUserId: String? = null
    private var fetchedHotelName: String? = null
    private var hotelPhone: String? = null
    private var hotelAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // ✅ Read data from intent (support both key formats)
        foodName = intent.getStringExtra("MenuItemName") ?: intent.getStringExtra("foodName")
        foodDescription = intent.getStringExtra("MenuItemDescription") ?: intent.getStringExtra("foodDescription")
        foodIngredients = intent.getStringExtra("MenuItemIngredients") ?: intent.getStringExtra("foodIngredients")
        foodPrice = intent.getStringExtra("MenuItemPrice") ?: intent.getStringExtra("foodPrice")
        foodImage = intent.getStringExtra("MenuItemImage") ?: intent.getStringExtra("foodImage")
        hotelUserId = intent.getStringExtra("HotelUserId") ?: intent.getStringExtra("hotelUserId")
        fetchedHotelName = intent.getStringExtra("HotelName") ?: intent.getStringExtra("hotelName")

        // ✅ Set UI content
        binding.detailefoodname.text = foodName ?: "No Name"
        binding.descriptiontext.text = foodDescription ?: "No Description"
        binding.ingridientstext.text = foodIngredients ?: "No Ingredients"

        Glide.with(this)
            .load(Uri.parse(foodImage ?: ""))
            .into(binding.descriptionimage)

        binding.descriptionimage.setOnClickListener { finish() }

        binding.addtocartbutton.setOnClickListener { addItemToCart() }

        binding.gotocartbutton.setOnClickListener {
            startActivity(Intent(this@DetailsActivity, CartActivity::class.java))
            finish()
        }

        // ✅ Fetch hotel details if user ID is available
        if (!hotelUserId.isNullOrEmpty()) {
            fetchHotelDetails(hotelUserId!!)
        } else {
            // Fallback UI
            binding.restaurantNameText.text = fetchedHotelName ?: "N/A"
            binding.restaurantAddressText.text = "N/A"
            binding.restaurantPhoneText.text = "N/A"
        }
    }

    private fun fetchHotelDetails(hotelUserId: String) {
        val databaseRef = FirebaseDatabase.getInstance()
            .getReference("Hotel Users")
            .child(hotelUserId)

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                fetchedHotelName = snapshot.child("nameOfResturant").getValue(String::class.java) ?: fetchedHotelName
                hotelPhone = snapshot.child("phone").getValue(String::class.java)
                hotelAddress = snapshot.child("address").child("address").getValue(String::class.java)
                    ?: snapshot.child("address").getValue(String::class.java)

                binding.restaurantNameText.text = fetchedHotelName ?: "N/A"
                binding.restaurantAddressText.text = hotelAddress ?: "N/A"
                binding.restaurantPhoneText.text = hotelPhone ?: "N/A"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DetailsActivity, "Failed to load hotel info", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addItemToCart() {
        val database = FirebaseDatabase.getInstance().reference
        val userId = auth.currentUser?.uid ?: return

        val cartItem = CartItems(
            foodNames = foodName,
            foodPrice = foodPrice,
            foodDescriptions = foodDescription,
            foodImage = foodImage,
            foodQuantity = 1,
            foodIngredients = foodIngredients,
            hotelName = fetchedHotelName,
            hotelUserId = hotelUserId // ✅ Include hotelUserId
        )

        database.child("user").child(userId).child("CartItems").push()
            .setValue(cartItem)
            .addOnSuccessListener {
                Toast.makeText(this, "Item Added to Cart", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to Add Item", Toast.LENGTH_SHORT).show()
            }
    }
}
