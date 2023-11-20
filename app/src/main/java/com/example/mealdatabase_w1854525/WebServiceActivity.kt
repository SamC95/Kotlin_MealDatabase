package com.example.mealdatabase_w1854525

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class WebServiceActivity: AppCompatActivity() {
    private val searchActivity = SearchActivity()

    private lateinit var searchTextField: EditText
    private lateinit var nextMealButton: Button
    private lateinit var prevMealButton: Button
    private lateinit var mealTextInfo: TextView
    private lateinit var noMealFound: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var searchButton: Button

    private var mealInfoPosition = 0
    private var searchPressed = false

    private var mealData = mutableListOf<String>()
    private var stringBuilder = StringBuilder("")
    private var urlString: String? = null

    private var toastChecker = true;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webservice)

        val apiKey = resources.getString(R.string.MY_API_KEY)

        searchTextField = findViewById(R.id.searchMealEditText)
        nextMealButton = findViewById(R.id.nextMeal)
        prevMealButton = findViewById(R.id.prevMeal)
        mealTextInfo = findViewById(R.id.mealInfo)
        noMealFound = findViewById(R.id.noMeal)
        scrollView = findViewById(R.id.mealListInfo)
        searchButton = findViewById(R.id.searchButton)

        scrollView.isVisible = false
        noMealFound.isVisible = false
        nextMealButton.isVisible = false
        prevMealButton.isVisible = false
        mealTextInfo.isVisible = false

        // If orientation changes, get the necessary element/variable states to retain the same data
        if (savedInstanceState != null) {
            // Gets the meal data in arraylist form and saves it to a temp variable
            val tempMealData = savedInstanceState.getStringArrayList("mealData")

            // If the data is not null, stores it back into mealData as a mutable list
            if (tempMealData != null) {
                mealData = tempMealData.toMutableList()
            }

            // Gets the current meal position and meal info
            mealInfoPosition = savedInstanceState.getInt("mealInfoPosition")
            mealTextInfo.text = savedInstanceState.getString("mealTextInfo")

            // Gets the current visibility status for certain UI elements
            nextMealButton.isVisible = savedInstanceState.getBoolean("nextMealVisibility")
            prevMealButton.isVisible = savedInstanceState.getBoolean("prevMealVisibility")
            scrollView.isVisible = savedInstanceState.getBoolean("scrollViewVisibility")
            mealTextInfo.isVisible = savedInstanceState.getBoolean("mealTextInfoVisibility")
            noMealFound.isVisible = savedInstanceState.getBoolean("noMealsVisibility")

            // Gets the boolean to check if user has pressed search at least once
            searchPressed = savedInstanceState.getBoolean("searchPressed")

            /* If user has searched at least once,
            checks if the meal info should be visible or no meals found should be visible*/
            if (searchPressed)
                checkMealVisibility(mealData)
        }

        // When search button is pressed
        searchButton.setOnClickListener {
            // Resets meal position to default state on repeated presses
            mealInfoPosition = 0
            searchPressed = true

            // Hides UI elements
            scrollView.isVisible = false
            noMealFound.isVisible = false
            nextMealButton.isVisible = false
            prevMealButton.isVisible = false
            mealTextInfo.isVisible = false

            // Stores the search input as a variable
            val searchInput = searchTextField.text.toString().trim()

            // If the input is empty, return and do nothing
            if (searchInput == "")
                return@setOnClickListener

            // Sets URL to string variable with api key and search input
            urlString = "https://www.themealdb.com/api/json/v1/$apiKey/search.php?s=$searchInput"

            // Starts new coroutine for connecting to the web API
            runBlocking {
                withContext(Dispatchers.IO) {
                    // Resets certain data on each run through (when search is pressed)
                    stringBuilder = StringBuilder("")
                    mealData.clear()

                    // Creates URL from the URL string abd opens a connection
                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    val reader: BufferedReader

                    // Checks if buffered reader works correctly
                    try {
                        reader = BufferedReader(InputStreamReader(connection.inputStream))
                    }
                    catch (error: IOException) {
                        error.printStackTrace()
                        return@withContext
                    }

                    // Reads current line from retrieved data
                    var currentLine = reader.readLine()

                    // Read through each line and append to string builder until line is empty
                    while (currentLine != null) {
                        stringBuilder.append(currentLine)
                        currentLine = reader.readLine()
                    }
                    // Close the reader and disconnect from the URL connection
                    reader.close()
                    connection.disconnect()

                    // If the string builder contains at least one meal, parse the JSON data
                    if (stringBuilder.length > 15) {
                        mealData = parseJSON(stringBuilder)
                    }
                }
            }
            // Check if the meal info should be visible or the no meals found message should be visible
            checkMealVisibility(mealData)
        }

        /*
         When next button is pressed, change the text to the next position in the mutable list
         and update the mealInfoPosition counter accordingly
        */
        nextMealButton.setOnClickListener {
            try {
                if (mealInfoPosition != mealData.size) {
                    mealTextInfo.text = mealData[mealInfoPosition + 1]
                    mealInfoPosition++
                }
            }
            // If user presses next at end of mutable list, catch error and inform user with toast msg
            catch (error: IndexOutOfBoundsException) {
                if (toastChecker) {
                    Toast.makeText(applicationContext, "No meals remaining", Toast.LENGTH_LONG).show()
                    toastChecker = false
                }
            }
        }

        /*
            If not on first meal position, pressing previous meal will move backwards in the
            mutable list and update the meal position accordingly
        */
        prevMealButton.setOnClickListener {
            if (mealInfoPosition != 0) {
                mealTextInfo.text = mealData[mealInfoPosition - 1]
                mealInfoPosition--
                toastChecker = true
            }
        }
    }

    // Saves all necessary data into onSaveInstanceState so that it can be retained upon orientation change
    override fun onSaveInstanceState(outState: Bundle) {
        // Sets mealData mutable list into a StringArrayList and stores it
        val mealArrayList = ArrayList<String>(mealData)

        outState.putStringArrayList("mealData", mealArrayList)

        // Stores current value of meal position counter
        outState.putInt("mealInfoPosition", mealInfoPosition)

        // Stores current state of some UI elements visibility
        outState.putBoolean("nextMealVisibility", nextMealButton.isVisible)
        outState.putBoolean("prevMealVisibility", prevMealButton.isVisible)
        outState.putBoolean("scrollViewVisibility", scrollView.isVisible)
        outState.putBoolean("mealTextInfoVisibility", mealTextInfo.isVisible)
        outState.putBoolean("noMealsVisibility", noMealFound.isVisible)

        // Stores check if user has searched at least once
        outState.putBoolean("searchPressed", searchPressed)

        super.onSaveInstanceState(outState)
    }


    private fun parseJSON(stringBuilder: StringBuilder): MutableList<String> {
        val mealTextInfo = mutableListOf<String>()

        val json = JSONObject(stringBuilder.toString())
        val mealList = json.getJSONArray("meals")
        var mealInfo: String

        for (i in 0 until mealList.length()) {
            val mealObject = mealList.getJSONObject(i)
            val ingredients = mutableListOf<String>()
            val measurements = mutableListOf<String>()

            for (j in 0 until 20) {
                val ingredient = mealObject.getString("strIngredient${j+1}")
                val measurement = mealObject.getString("strMeasure${j+1}")

                if (ingredient.isNotEmpty()) {
                    ingredients.add(ingredient)
                }
                else {
                    ingredients.add(null.toString())
                }

                if (measurement.isNotEmpty()) {
                    measurements.add(measurement)
                }

                else {
                    measurements.add(null.toString())
                }
            }

            val mealDetails = searchActivity.addToMealArray(mealObject, ingredients, measurements)

            mealInfo =
                "Meal: " + mealDetails.meal +
                        "\nDrinkAlternate: " + mealDetails.drinkAlternate +
                        "\nCategory: " + mealDetails.category +
                        "\nArea: " + mealDetails.area +
                        "\nInstructions: " + (mealDetails.instructions?.substring(0, 30)) + ".... " +
                        "\nTags: " + mealDetails.tags +
                        "\nYoutube: " + mealDetails.youtubeVideo + "\n"

            for (j in 0 until ingredients.size) {
                if (ingredients[j].isNotEmpty() && ingredients[j] != "null") {
                    mealInfo += "Ingredient${j + 1}: ${ingredients[j]}\n"
                }
            }
            for (j in 0 until measurements.size) {
                if (measurements[j].isNotEmpty() && (measurements[j] != "null" && measurements[j] != " ")) {
                    mealInfo += "Measure${j + 1}: ${measurements[j]}\n"
                }
            }

            mealTextInfo.add(mealInfo)
        }
        return mealTextInfo
    }

    private fun checkMealVisibility(mealData: MutableList<String>) {
        if (mealData.size > 0) {
            scrollView.isVisible = true
            mealTextInfo.text = mealData[mealInfoPosition]
            mealTextInfo.isVisible = true
            nextMealButton.isVisible = true
            prevMealButton.isVisible = true
        }
        else {
            noMealFound.text = resources.getString(R.string.no_match_meal_or_ingredient)
            noMealFound.isVisible = true
        }
    }
}