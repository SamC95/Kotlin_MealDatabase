package com.example.mealdatabase_w1854525

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Parcelable
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class SearchMealActivity: AppCompatActivity() {

    // Variables for UI elements
    private lateinit var searchTextField: EditText
    private lateinit var searchMealsButton: Button
    private lateinit var nextMealButton: Button
    private lateinit var prevMealButton: Button
    private lateinit var mealTextInfo: TextView
    private lateinit var noMealFound: TextView
    private lateinit var mealImage: ImageView
    private lateinit var scrollView: ScrollView

    // Counter to keep track of current meal in array list
    private var mealInfoPosition = 0

    // List of Meal objects and a list of meal strings
    private var mealList = mutableListOf<Meal>()
    private var mealStringList = mutableListOf<String>()
    private var image: Bitmap? = null
    private var searchText: String = ""

    private var toastChecker = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_search)

        // Build database for use in this activity
        val database = Room.databaseBuilder(
            this, AppDatabase::class.java,
            "mealDatabase"
        ).build()
        val mealDao = database.mealDao()

        // Initialises the UI elements
        searchTextField = findViewById(R.id.searchMealEditText)
        searchMealsButton = findViewById(R.id.searchButton)
        nextMealButton = findViewById(R.id.nextMeal)
        prevMealButton = findViewById(R.id.prevMeal)
        mealTextInfo = findViewById(R.id.mealInfo)
        noMealFound = findViewById(R.id.noMeal)
        mealImage = findViewById(R.id.mealImage)
        scrollView = findViewById(R.id.mealListInfo)

        // Hides specific UI elements on initial activity start
        nextMealButton.isVisible = false
        prevMealButton.isVisible = false
        noMealFound.isVisible = false
        mealImage.isVisible = false
        scrollView.isVisible = false

        /*
         Restores necessarily elements and variable states upon orientation change so that correct
         data is retained and the user's experience is not interrupted
        */
        if (savedInstanceState != null) {
            /*
            Takes the array of bytes for the image data that was stored and decodes it back into
            a bitmap format so that it can be displayed again normally
            */
            val arrayOfBytes = savedInstanceState.getByteArray("mealImage")
            if (arrayOfBytes != null) {
                image = BitmapFactory.decodeByteArray(arrayOfBytes, 0, arrayOfBytes.size)
                mealImage.setImageBitmap(image)
            }

            /* Takes the string arraylist, stores it in a temp variable and then converts it back
            *  to a mutable list */
            val tempMealList = savedInstanceState.getStringArrayList("mealStringList")

            if (tempMealList != null) {
                mealStringList = tempMealList.toMutableList()
            }

            // Restores the current meal position counter, meal information and last search request
            mealInfoPosition = savedInstanceState.getInt("mealPosition")
            mealTextInfo.text = savedInstanceState.getString("mealTextInfo")
            searchText = savedInstanceState.getString("searchText").toString()

            // Restores the state of certain UI elements visibility
            nextMealButton.isVisible = savedInstanceState.getBoolean("nextMealVisibility")
            prevMealButton.isVisible = savedInstanceState.getBoolean("prevMealVisibility")
            scrollView.isVisible = savedInstanceState.getBoolean("scrollViewVisibility")
            mealTextInfo.isVisible = savedInstanceState.getBoolean("mealTextInfoVisibility")
            noMealFound.isVisible = savedInstanceState.getBoolean("noMealsVisibility")
            mealImage.isVisible = savedInstanceState.getBoolean("mealImageVisibility")

            // If the search text was not an empty string, gets the appropriate data from the database
            if (searchText != "%%") {
                getFromDatabase(mealDao, searchText)
            }

            checkMealVisibility()
        }

        searchMealsButton.setOnClickListener {
            // Resets counter on search press and resets certain UI elements to hidden
            mealInfoPosition = 0

            mealImage.isVisible = false
            scrollView.isVisible = false
            noMealFound.isVisible = false
            nextMealButton.isVisible = false
            prevMealButton.isVisible = false
            mealTextInfo.isVisible = false

            /* Gets the user input, trims it and assigns it to variable, place % at front and end
            * for partial match in SQL Query
            * Search Field is also set on XML side to only allow a-z and A-Z characters, not numbers
            * or special characters */
            searchText = "%" + searchTextField.text.toString().trim() + "%"

            // If user input is empty, return and do nothing
            if (searchText == "%%")
                return@setOnClickListener

            getFromDatabase(mealDao, searchText)

            /* If the meal string list isn't empty, displays the scroll view and meal info,
            *  as well as the Next Meal and Previous Meal buttons */
            if (mealStringList.size > 0) {
                scrollView.isVisible = true
                mealTextInfo.text = mealStringList[mealInfoPosition]
                mealTextInfo.isVisible = true
                nextMealButton.isVisible = true
                prevMealButton.isVisible = true

                /* Calls the getImage function to get the appropriate image for the meal based on
                 the current position in the list*/
                image = getImage(mealList, mealInfoPosition)
                mealImage.setImageBitmap(image)
                mealImage.isVisible = true
            }
            else {
                // If no meals found, displays a text view to inform the user
                noMealFound.text = resources.getString(R.string.no_match)
                noMealFound.isVisible = true
            }
        }

        /*When the user presses Next Meal, the meal info will change to the next meal in the
        * list of meal strings, and the getImage function will be called again to get the image
        * for the next meal position. If the user reaches the end of the meal list, a toast
        * appears to inform the user and prevent an index out of bounds exception */
        nextMealButton.setOnClickListener {
            try {
                if (mealInfoPosition != mealStringList.size) {
                    mealTextInfo.text = mealStringList[mealInfoPosition + 1]
                    mealInfoPosition++
                    image = getImage(mealList, mealInfoPosition)
                    mealImage.setImageBitmap(image)
                }
            }
            catch (error: IndexOutOfBoundsException) {
                if (toastChecker) {
                    Toast.makeText(applicationContext, "No meals remaining", Toast.LENGTH_LONG).show()
                    toastChecker = false
                }
            }
        }

        /*
         If the user presses the previous meal button whilst not on the first meal in the list,
         the meal info will be displayed for one place backwards in the list of meal strings and
         the getImage function is called again to get the image for the previous meal position.
        */
        prevMealButton.setOnClickListener {
            if (mealInfoPosition != 0) {
                mealTextInfo.text = mealStringList[mealInfoPosition - 1]
                mealInfoPosition--
                image = getImage(mealList, mealInfoPosition)
                mealImage.setImageBitmap(image)
                toastChecker = true
            }
        }
    }

    // Saves all necessary data into onSaveInstanceState so that it can be retained upon orientation change
    override fun onSaveInstanceState(outState: Bundle) {
        // Stores the mutable list of meal info into a StringArrayList and stores it
        val mealStringArrayList = ArrayList<String>(mealStringList)

        outState.putStringArrayList("mealTextStringList", mealStringArrayList)

        // Stores the current UI visibility of certain elements
        outState.putBoolean("nextMealVisibility", nextMealButton.isVisible)
        outState.putBoolean("prevMealVisibility", prevMealButton.isVisible)
        outState.putBoolean("scrollViewVisibility", scrollView.isVisible)
        outState.putBoolean("mealTextInfoVisibility", mealTextInfo.isVisible)
        outState.putBoolean("noMealsVisibility", noMealFound.isVisible)
        outState.putBoolean("mealImageVisibility", mealImage.isVisible)

        // Stores the current meal position, current meal info and last search request
        outState.putInt("mealPosition", mealInfoPosition)
        outState.putString("mealTextInfo", mealTextInfo.text.toString())
        outState.putString("searchText", searchText)

        // Converts and compresses the image bitmap into an array of bytes so that it can be stored
        val stream = ByteArrayOutputStream()
        image?.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val arrayOfBytes = stream.toByteArray()
        outState.putByteArray("mealImage", arrayOfBytes)

        super.onSaveInstanceState(outState)
    }

    /* Takes the meal object and creates a meal string for it in the appropriate format to be displayed
    * to the user */
    private fun formatMeal(mealList: MutableList<Meal>): MutableList<String> {
        val mealText = mutableListOf<String>()
        var mealInfo: String

        for (meal in mealList) {

            mealInfo =
                "Meal: " + meal.meal +
                        "\nDrinkAlternate: " + meal.drinkAlternate +
                        "\nCategory: " + meal.category +
                        "\nArea: " + meal.area +
                        "\nInstructions: " + (meal.instructions?.substring(0, 30)) + ".... " +
                        "\nTags: " + meal.tags +
                        "\nYoutube: " + meal.youtubeVideo +
                        "\nIngredient1: " + meal.ingredient1 + "\nIngredient2: " + meal.ingredient2 +
                        "\nIngredient3: " + meal.ingredient3 + "\nIngredient4: " + meal.ingredient4 +
                        "\nIngredient5: " + meal.ingredient5 + "\nIngredient6: " + meal.ingredient6 +
                        "\nIngredient7: " + meal.ingredient7 + "\nIngredient8: " + meal.ingredient8 +
                        "\nIngredient9: " + meal.ingredient9 + "\nIngredient10: " + meal.ingredient10 +
                        "\nIngredient11: " + meal.ingredient11 + "\nIngredient12: " + meal.ingredient12 +
                        "\nIngredient13: " + meal.ingredient13 + "\nIngredient14: " + meal.ingredient14 +
                        "\nIngredient15: " + meal.ingredient15 + "\nIngredient16: " + meal.ingredient16 +
                        "\nIngredient17: " + meal.ingredient17 + "\nIngredient18: " + meal.ingredient18 +
                        "\nIngredient19: " + meal.ingredient19 + "\nIngredient20: " + meal.ingredient20 +
                        "\nMeasure1: " + meal.measure1 + "\nMeasure2: " + meal.measure2 +
                        "\nMeasure3: " + meal.measure3 + "\nMeasure4: " + meal.measure4 +
                        "\nMeasure5: " + meal.measure5 + "\nMeasure6: " + meal.measure6 +
                        "\nMeasure7: " + meal.measure7 + "\nMeasure8: " + meal.measure8 +
                        "\nMeasure9: " + meal.measure9 + "\nMeasure10: " + meal.measure10 +
                        "\nMeasure11: " + meal.measure11 + "\nMeasure12: " + meal.measure12 +
                        "\nMeasure13: " + meal.measure13 + "\nMeasure14: " + meal.measure14 +
                        "\nMeasure15: " + meal.measure15 + "\nMeasure16: " + meal.measure16 +
                        "\nMeasure17: " + meal.measure17 + "\nMeasure18: " + meal.measure18 +
                        "\nMeasure19: " + meal.measure19 + "\nMeasure20: " + meal.measure20 + "\n"

            /*
             Checks through the meal info line by line to see if any of the ingredient or measurement
             fields are empty, if they are empty then the line is removed from the string so that
             empty fields are not displayed to the user unnecessarily
            */
            for (line in mealInfo.split("\n")) {
                if (line.contains("Ingredient") || line.contains("Measure")) {
                    val splitLine = line.split(":")
                    if (splitLine[1].trim().isEmpty() || splitLine[1] == " null") {
                        mealInfo = mealInfo.replace(line + "\n", "")
                    }
                }
            }
            mealText.add(mealInfo)
        }
        return mealText
    }

    /*Gets the image from the mealdb website by getting the link from the room database for the
    * current meal and assigning it to a URL which is then opened and the image retrieved assigned
    * to a bitmap image to then be displayed on an ImageView UI element*/
    private fun getImage(mealList: MutableList<Meal>, mealInfoPosition: Int): Bitmap? {
        var bitmap: Bitmap? = null

        runBlocking {
            withContext(Dispatchers.IO) {
                for ((index, meal) in mealList.withIndex()) {
                    if (index == mealInfoPosition) {
                        val url = URL(meal.mealThumbnail)
                        val connect = url.openConnection() as HttpURLConnection
                        val buffer = BufferedInputStream(connect.inputStream)
                        bitmap = BitmapFactory.decodeStream(buffer)
                    }
                }
            }
        }
        return bitmap
    }

    // Starts new coroutine block
    private fun getFromDatabase(mealDao: MealDao, searchText: String) {
        runBlocking {
            withContext(Dispatchers.IO) {
                // Gets all meals that match the user input from the room database
                mealList = mealDao.findMealByNameOrIngredient(searchText)

                // Formats the meal info to a string and assigns it to the list of meal strings
                mealStringList = formatMeal(mealList)
            }
        }
    }

    private fun checkMealVisibility() {
        /* If the meal string list isn't empty, displays the scroll view and meal info,
            *  as well as the Next Meal and Previous Meal buttons */
        if (mealStringList.size > 0) {
            scrollView.isVisible = true
            mealTextInfo.text = mealStringList[mealInfoPosition]
            mealTextInfo.isVisible = true
            nextMealButton.isVisible = true
            prevMealButton.isVisible = true

            /* Calls the getImage function to get the appropriate image for the meal based on
             the current position in the list*/
            image = getImage(mealList, mealInfoPosition)
            mealImage.setImageBitmap(image)
            mealImage.isVisible = true
        }
        else {
            // If no meals found, displays a text view to inform the user
            noMealFound.text = resources.getString(R.string.no_match)
            noMealFound.isVisible = true
        }
    }
}