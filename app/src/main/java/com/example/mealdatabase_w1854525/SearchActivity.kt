package com.example.mealdatabase_w1854525

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class SearchActivity: AppCompatActivity() {
    // variables for UI elements and URL strings
    private lateinit var retrieveMealsButton: Button
    private lateinit var saveMealsButton: Button
    private lateinit var searchIngredient: EditText
    private lateinit var scrollView: ScrollView
    private lateinit var mealInfoTextView: TextView
    private lateinit var noMealFound: TextView
    private lateinit var nextMealButton: Button
    private lateinit var prevMealButton: Button
    private var urlIdString: String? = null
    private var urlMealString: String? = null

    // Variable to determine the current position in the list of meals to be displayed
    private var mealInfoPosition: Int = 0

    // Boolean to check if user has pressed retrieve meals at least once
    private var mealsRetrieved = false
    private var savePressed = false

    // Lists for the array of meal IDs, meals and formatted strings of meals
    private lateinit var arrayOfIds: MutableList<String>
    private lateinit var retrievedData: MutableList<String>
    private lateinit var mealArray: MutableList<Meal>

    private var stringBuilder = StringBuilder("")
    private var ingredient: String = ""

    private var toastChecker = true;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingredient_search)

        // Builds database for this activity
        val database = Room.databaseBuilder(
            this, AppDatabase::class.java,
            "mealDatabase"
        ).build()
        val mealDao = database.mealDao()

        // Assigns variables to specific UI elements
        retrieveMealsButton = findViewById(R.id.retrieveMeals)
        saveMealsButton = findViewById(R.id.saveMeals)
        nextMealButton = findViewById(R.id.nextMeal)
        prevMealButton = findViewById(R.id.prevMeal)
        searchIngredient = findViewById(R.id.ingredientSearch)
        mealInfoTextView = findViewById(R.id.mealInfo)
        noMealFound = findViewById(R.id.noMeal)
        scrollView = findViewById(R.id.mealListInfo)

        // Assigns api key to variable
        val apiKey = resources.getString(R.string.MY_API_KEY)

        var mealCounter = 0

        // Sets UI elements to invisible upon activity start
        nextMealButton.isVisible = false
        prevMealButton.isVisible = false
        scrollView.isVisible = false

        if (savedInstanceState != null) {
            val mealStringInfo = savedInstanceState.getString("stringBuilder")
            stringBuilder.append(mealStringInfo)

            mealInfoPosition = savedInstanceState.getInt("mealInfoPosition")

            nextMealButton.isVisible = savedInstanceState.getBoolean("nextMealVisibility")
            prevMealButton.isVisible = savedInstanceState.getBoolean("prevMealVisibility")
            scrollView.isVisible = savedInstanceState.getBoolean("scrollViewVisibility")
            mealInfoTextView.isVisible = savedInstanceState.getBoolean("mealTextInfoVisibility")
            noMealFound.isVisible = savedInstanceState.getBoolean("noMealsVisibility")
            mealsRetrieved = savedInstanceState.getBoolean("mealsRetrieved")
            savePressed = savedInstanceState.getBoolean("savePressed")

            // Gets the last search the user made and stores it back into the editText to re-collect meal data
            ingredient = savedInstanceState.getString("searchIngredient").toString()
            searchIngredient.setText(ingredient)

            val (tempData, tempMealArray) = parseMealJSON(stringBuilder)

            mealArray = tempMealArray
            retrievedData = tempData

            checkMealVisibility(retrievedData)
        }

        retrieveMealsButton.setOnClickListener {
            // Resets UI elements and meal position back to defaults upon retrieve meals being pressed
            mealInfoPosition = 0
            mealCounter = 0
            savePressed = false
            mealsRetrieved = true

            scrollView.isVisible = false
            noMealFound.isVisible = false
            nextMealButton.isVisible = false
            prevMealButton.isVisible = false
            mealInfoTextView.isVisible = false

            // Gets text from user input and assigns it to ingredient variable
            ingredient = searchIngredient.text.toString().trim()

            // if user input was empty, return and do nothing
            if (ingredient == "")
                return@setOnClickListener

            // Assigns URL for the mealdb api, ingredient changes based on user input to get the correct API request
            urlIdString = "https://www.themealdb.com/api/json/v1/$apiKey/filter.php?i=$ingredient"

            // Creates new thread
            runBlocking {
                withContext(Dispatchers.IO) {
                    val idFinder = StringBuilder("")
                    stringBuilder = StringBuilder("")

                    // Creates URL variable and opens HTTP connection to the given url
                    val idUrl = URL(urlIdString)
                    val connection = idUrl.openConnection() as HttpURLConnection
                    val idReader: BufferedReader

                    arrayOfIds = mutableListOf()

                    // Checks that bufferedReader connects to HTTP connection correctly
                    try {
                        idReader = BufferedReader(InputStreamReader(connection.inputStream))
                    } catch (error: IOException) {
                        error.printStackTrace()
                        return@withContext
                    }

                    // Variable for reading the current line
                    var currentLine = idReader.readLine()

                    // Loops until current line is empty, appending each line onto the currentLine variable
                    while (currentLine != null) {
                        idFinder.append(currentLine)
                        currentLine = idReader.readLine()
                    }

                    // Will only call parseIdJSON if idFinder contains at least one meal, to prevent errors from null value
                    if (idFinder.toString().length > 15) {
                        arrayOfIds = parseIdJSON(idFinder)
                    }

                    // Finds each meal based on the ID in the array of IDs through the same process done previously
                    var mealReader: BufferedReader
                    for (i in 0 until arrayOfIds.size) {
                        urlMealString = "https://www.themealdb.com/api/json/v1/$apiKey/lookup.php?i=${arrayOfIds[i]}"

                        val mealUrl = URL(urlMealString)
                        val mealConnection = mealUrl.openConnection() as HttpURLConnection

                        try {
                            mealReader = BufferedReader(InputStreamReader(mealConnection.inputStream))
                        } catch (error: IOException) {
                            error.printStackTrace()
                            return@withContext
                        }

                        currentLine = mealReader.readLine()

                        /*For each line, removes certain parts of the retrieved data so that it connects
                          together into one JSONObject correctly*/
                        if (currentLine != null && i != arrayOfIds.size - 1) {
                            currentLine = currentLine.replace("\"meals\":[{", "")
                            currentLine = currentLine.replace("]}", ",")
                        }
                        else if (currentLine!= null && i != 0) {
                            currentLine = currentLine.replace("\"meals\":[{", "")
                        }

                        // Appends each line onto the stringBuilder variable
                        while (currentLine != null) {
                            stringBuilder.append(currentLine)
                            currentLine = mealReader.readLine()
                            mealCounter++
                        }
                        // Closes the reader and connection
                        mealReader.close()
                        mealConnection.disconnect()
                    }


                    // Re-adds certain JSON formatting at the start and end of the entire string
                    if (mealCounter > 1) {
                        stringBuilder.insert(0, "{\"meals\":[")
                        stringBuilder.append("]}")
                    }

                    /*
                    calls parseMealJSON object and returns an array list of meal objects and a
                    mutable list of strings of each meal
                    */
                    val (tempData, tempMealArray) = parseMealJSON(stringBuilder)

                    // Assigns array list/mutable list to new variables
                    mealArray = tempMealArray
                    retrievedData = tempData
                }
            }

            // Determines if the program should display meal info or no meal found message
            checkMealVisibility(retrievedData)
        }

        /* Saves meals into the room database based on what was added into the arraylist of meals in parseMealJSON
        * If the user has not pressed retrieve meal at least once, text will inform the user that no
        * meals were saved, this message will also occur if the user attempts to save to database
        * on a search where no results were found */
        saveMealsButton.setOnClickListener {
            savePressed = true
            if (mealsRetrieved) {
                runBlocking {
                    launch {
                        if (mealArray.size > 0) {
                            for (i in 0 until mealArray.size) {
                                mealDao.insertMeals(mealArray[i])
                            }
                            Toast.makeText(applicationContext, "Meals Added", Toast.LENGTH_LONG).show()
                            val meals: List<Meal> = mealDao.getAll()
                            for (m in meals) {
                                Log.d("******", "" + m.meal + ", " + m.category)
                            }
                        }
                        else {
                            noMealFound.text = resources.getString(R.string.no_save)
                        }
                    }
                }
            }
            else {
                noMealFound.text = resources.getString(R.string.no_save)
                noMealFound.isVisible = true
            }
        }

        // Displays the next meal in the mutable list of strings and updates the counter of mealInfoPosition appropriately
        nextMealButton.setOnClickListener {
            try {
                if (mealInfoPosition != retrievedData.size) {
                    mealInfoTextView.text = retrievedData[mealInfoPosition + 1]
                    mealInfoPosition++
                }
            }
            // Displays a toast to the user when they press next at the end of the mutable list, to prevent crash
            catch (error: IndexOutOfBoundsException) {
                if (toastChecker) {
                    Toast.makeText(applicationContext, "No meals remaining", Toast.LENGTH_LONG).show()
                    toastChecker = false;
                }
            }
        }

        // Displays the previous meal in the mutable list of strings if not at the first meal in the list
        prevMealButton.setOnClickListener {
            if (mealInfoPosition != 0) {
                mealInfoTextView.text = retrievedData[mealInfoPosition - 1]
                mealInfoPosition--
                toastChecker = true;
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("stringBuilder", stringBuilder.toString())
        outState.putString("searchIngredient", ingredient)

        outState.putInt("mealInfoPosition", mealInfoPosition)
        outState.putBoolean("mealsRetrieved", mealsRetrieved)
        outState.putBoolean("savePressed", savePressed)

        outState.putBoolean("nextMealVisibility", nextMealButton.isVisible)
        outState.putBoolean("prevMealVisibility", prevMealButton.isVisible)
        outState.putBoolean("scrollViewVisibility", scrollView.isVisible)
        outState.putBoolean("mealTextInfoVisibility", mealInfoTextView.isVisible)
        outState.putBoolean("noMealsVisibility", noMealFound.isVisible)

        super.onSaveInstanceState(outState)
    }

    /*Takes the IDs from the ingredient search and stores them into an array of ID numbers to be
    used to find the full meal info for those IDs in parseMealJSON*/
    private fun parseIdJSON(idFinder: StringBuilder): MutableList<String> {
        val idSearcher = JSONObject(idFinder.toString())
        val listOfMatches = idSearcher.getJSONArray("meals")
        val idArray = mutableListOf<String>()
        idArray.clear()

        for (i in 0 until listOfMatches.length()) {
            val ingredientObject = listOfMatches.getJSONObject(i)
            val idNum = ingredientObject.getString("idMeal")

            idArray.add(idNum)
        }
        return idArray
    }

    private fun parseMealJSON(stringBuilder: StringBuilder): Pair<MutableList<String>, ArrayList<Meal>> {
        val mealArray = arrayListOf<Meal>()
        val mealTextInfo = mutableListOf<String>()

        if (stringBuilder.isNotEmpty()) {
            val json = JSONObject(stringBuilder.toString())
            val listOfMeals = json.getJSONArray("meals")
            var mealInfo: String
            mealArray.clear()

            // For the amount of meals in the JSONArray
            for (i in 0 until listOfMeals.length()) {
                val mealObject = listOfMeals.getJSONObject(i)
                val ingredients = mutableListOf<String>()
                val measurements = mutableListOf<String>()

                /* Gets each ingredient and measurement from the current JSONObject and adds
                them if it is not empty, if current ingredient or measurement is empty then adds null instead*/
                for (j in 0 until 20) {
                    val ingredient = mealObject.getString("strIngredient${j + 1}")
                    val measurement = mealObject.getString("strMeasure${j + 1}")
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
                // Creates a Meal object that can be saved to the database later
                val mealDetails = addToMealArray(mealObject, ingredients, measurements)

                /*If the ingredients list contains the current search by the user
                * Creates a string of the meal information in required format */
                for (ingredient in ingredients) {
                    if (ingredient.equals(searchIngredient.text.toString().trim(), ignoreCase = true)
                    ) {
                        mealInfo =
                            "Meal: " + mealDetails.meal +
                                    "\nDrinkAlternate: " + mealDetails.drinkAlternate +
                                    "\nCategory: " + mealDetails.category +
                                    "\nArea: " + mealDetails.area +
                                    "\nInstructions: " + (mealDetails.instructions?.substring(0, 30)) + ".... " +
                                    "\nTags: " + mealDetails.tags +
                                    "\nYoutube: " + mealDetails.youtubeVideo + "\n"

                        /*For ingredients and measurements, loops through each and only gets the ones that
                    * are not empty or do not equal null, values without ingredients or measurements are
                    * excluded from string*/
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

                        Log.d("meal", mealInfo)
                        // Adds the meal string to mutable list and the Meal object to the arraylist of meals
                        mealArray.add(mealDetails)
                        mealTextInfo.add(mealInfo)
                    }
                }
            }
        }
        return Pair(mealTextInfo, mealArray)
    }

    /* Creates the Meal object for later database storage by getting all the columns for the current meal
     * that is being handled in the parseMealJSON function
     * This function is also used in WebServiceActivity to create a Meal Object*/
    fun addToMealArray(mealObject: JSONObject, ingredients: MutableList<String>, measurements: MutableList<String>
    ): Meal {
        return Meal(
            idMeal = mealObject.getInt("idMeal"),
            meal = mealObject.getString("strMeal"),
            drinkAlternate = mealObject.getString("strDrinkAlternate"),
            category = mealObject.getString("strCategory"),
            area = mealObject.getString("strArea"),
            instructions = mealObject.getString("strInstructions"),
            mealThumbnail = mealObject.getString("strMealThumb"),
            tags = mealObject.getString("strTags"),
            youtubeVideo = mealObject.getString("strYoutube"),
            ingredient1 = ingredients[0], ingredient2 = ingredients[1], ingredient3 = ingredients[2],
            ingredient4 = ingredients[3], ingredient5 = ingredients[4], ingredient6 = ingredients[5],
            ingredient7 = ingredients[6], ingredient8 = ingredients[7], ingredient9 = ingredients[8],
            ingredient10 = ingredients[9], ingredient11 = ingredients[10], ingredient12 = ingredients[11],
            ingredient13 = ingredients[12], ingredient14 = ingredients[13], ingredient15 = ingredients[14],
            ingredient16 = ingredients[15], ingredient17 = ingredients[16], ingredient18 = ingredients[17],
            ingredient19 = ingredients[18], ingredient20 = ingredients[19],
            measure1 = measurements[0], measure2 = measurements[1], measure3 = measurements[2],
            measure4 = measurements[3], measure5 = measurements[4], measure6 = measurements[5],
            measure7 = measurements[6], measure8 = measurements[7], measure9 = measurements[8],
            measure10 = measurements[9], measure11 = measurements[10], measure12 = measurements[11],
            measure13 = measurements[12], measure14 = measurements[13], measure15 = measurements[14],
            measure16 = measurements[15], measure17 = measurements[16], measure18 = measurements[17],
            measure19 = measurements[18], measure20 = measurements[19],
            source = mealObject.getString("strSource"),
            imageSource = mealObject.getString("strImageSource"),
            CreativeCommonsConfirmed = mealObject.getString("strCreativeCommonsConfirmed"),
            dateModified = mealObject.getString("dateModified")
        )
    }

    private fun checkMealVisibility(retrievedData: MutableList<String>) {
        // If the meal string is not empty, displays meal and shows the required UI elements
        if (retrievedData.size > 0) {
            scrollView.isVisible = true
            mealInfoTextView.text = retrievedData[mealInfoPosition]
            nextMealButton.isVisible = true
            prevMealButton.isVisible = true
            mealInfoTextView.isVisible = true
        }
        // If the meal string is empty, displays message that no meals were found
        else {
            if (!savePressed) {
                noMealFound.text = resources.getString(R.string.none_found)
                noMealFound.isVisible = true
            }
            else {
                noMealFound.text = resources.getString(R.string.no_save)
                noMealFound.isVisible = true
            }
        }
    }
}
