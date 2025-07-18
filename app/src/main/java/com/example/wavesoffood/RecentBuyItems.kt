package com.example.wavesoffood

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavesoffood.adapter.RecentBuyAdapter
import com.example.wavesoffood.databinding.ActivityRecentBuyItemsBinding
import com.example.wavesoffood.model.OrderDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class RecentBuyItems : AppCompatActivity() {

    private val binding by lazy {
        ActivityRecentBuyItemsBinding.inflate(layoutInflater)
    }

    private val allFoodNames = arrayListOf<String>()
    private val allFoodImages = arrayListOf<String>()
    private val allFoodPrices = arrayListOf<String>()
    private val allFoodQuantities = arrayListOf<Int>()
    private val allFoodDescriptions = arrayListOf<String>()   // ✅ NEW
    private val allFoodIngredients = arrayListOf<String>()    // ✅ NEW
    private val allHotelNames = arrayListOf<String>()
    private var orderDetailsList = arrayListOf<OrderDetails>()
    private val database = FirebaseDatabase.getInstance()
    private var hotelUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        val recentOrderItems = intent.getSerializableExtra("RecentBuyOrderItem") as? ArrayList<OrderDetails>
        if (recentOrderItems != null) {
            orderDetailsList = recentOrderItems
            for (order in orderDetailsList) {
                allFoodNames.addAll(order.foodNames ?: emptyList())
                allFoodImages.addAll(order.foodImages ?: emptyList())
                allFoodPrices.addAll(order.foodPrices ?: emptyList())
                allFoodQuantities.addAll(order.foodQuantities ?: emptyList())
                allFoodDescriptions.addAll(order.foodDescriptions ?: List(order.foodNames?.size ?: 0) { "No description" }) // ✅ fallback
                allFoodIngredients.addAll(order.foodIngredients ?: List(order.foodNames?.size ?: 0) { "No ingredients" })   // ✅ fallback

                val count = order.foodNames?.size ?: 0
                repeat(count) {
                    allHotelNames.add(order.hotelName ?: "N/A")
                }

                hotelUserId = order.hotelUserId
            }
        }

        setAdapter()
        checkIfAllDispatched()

        binding.markAllReceivedBtn.setOnClickListener {
            markAllOrdersAsReceived()
        }
    }

    private fun setAdapter() {
        binding.recentViewrecycler.layoutManager = LinearLayoutManager(this)
        binding.recentViewrecycler.adapter = RecentBuyAdapter(
            this,
            allFoodNames,
            allFoodImages,
            allFoodPrices,
            allFoodQuantities,
            allFoodDescriptions,     // ✅ passed now
            allFoodIngredients,      // ✅ passed now
            allHotelNames,
            hotelUserId
        )
    }

    private fun checkIfAllDispatched() {
        if (orderDetailsList.isEmpty()) {
            binding.markAllReceivedBtn.isEnabled = false
            return
        }

        var totalToCheck = 0
        var checkedCount = 0
        var allDispatched = true

        for (item in orderDetailsList) {
            val hotelUserId = item.hotelUserId
            val itemPushKey = item.itemPushkey

            if (hotelUserId != null && itemPushKey != null) {
                totalToCheck++

                val ref = database.reference
                    .child("Hotel Users")
                    .child(hotelUserId)
                    .child("CompletedOrder")
                    .child(itemPushKey)

                ref.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val orderAccepted = snapshot.child("orderAccepted").getValue(Boolean::class.java) ?: false
                        if (!orderAccepted) {
                            allDispatched = false
                        }
                        checkedCount++
                        if (checkedCount == totalToCheck) {
                            binding.markAllReceivedBtn.isEnabled = allDispatched
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        checkedCount++
                        if (checkedCount == totalToCheck) {
                            binding.markAllReceivedBtn.isEnabled = false
                        }
                    }
                })
            }
        }

        if (totalToCheck == 0) {
            binding.markAllReceivedBtn.isEnabled = false
        }
    }

    private fun markAllOrdersAsReceived() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        var successCount = 0
        val total = orderDetailsList.size

        for (item in orderDetailsList) {
            val hotelUserId = item.hotelUserId ?: continue
            val itemPushKey = item.itemPushkey ?: continue
            val userName = item.userNames ?: "Customer"

            val paymentRef = database.reference
                .child("Hotel Users").child(hotelUserId)
                .child("CompletedOrder").child(itemPushKey)
                .child("paymentReceived")

            paymentRef.setValue(true).addOnSuccessListener {
                successCount++
                if (successCount == total) {
                    Toast.makeText(this@RecentBuyItems, "All items marked as received!", Toast.LENGTH_SHORT).show()
                    binding.markAllReceivedBtn.isEnabled = false
                }

                val notificationRef = database.reference
                    .child("Hotel Users").child(hotelUserId)
                    .child("notifications").child(itemPushKey)

                val notifyMap = mapOf(
                    "title" to "Payment Received",
                    "message" to "Payment received from $userName",
                    "timestamp" to System.currentTimeMillis().toString()
                )

                notificationRef.setValue(notifyMap)
            }.addOnFailureListener {
                Toast.makeText(this@RecentBuyItems, "Failed to update some orders", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
