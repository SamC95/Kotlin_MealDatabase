# Android Meal Database Application
## Mobile Application Development - Kotlin

By [Sam Clark](https://github.com/SamC95)

This project was coursework for my second year module: Mobile Application Development - at the University of Westminster

## Contents
* [Project Brief]()
* [Technologies]()
* [Implementation]()
* [Key Learnings]()
* [Achievements]()
* [Challenges]()
* [Conclusions]()

## Project Brief
* You are required to implement an Android application using Kotlin.
* You are not allowed to use third-party libraries. The only libraries that you can
use are the standard Android API libraries.
* The application developed will be helping users with meal preparation.
* The application will be using the [themealdb](https://www.themealdb.com/api.php/) Web service and
the Room Library to save information about meals.

### Requirements
* When the application starts, it presents the user with 3 buttons labelled Add Meals to DB,
Search for Meals By Ingredient and Search for Meals.
* Clicking on the Add Meals to DB button saves all the details of a few meals in an SQLite
database local to the mobile device using the Room library. The specific information of
the meals which will be saved is shown in the following link and the information saved
could simply be hardcoded in the application.
* An appropriate database with appropriate tables should be created and populated by your
application, based on the above data.
* if the user clicks on the second button Save meals to Database all the
retrieved details of all the retrieved meals will be saved to the SQLite database of the device
(using the Room library), by using the same tables which were utilised in the previous
subquestion.
* Clicking on the Search for Meals button will display the user with a screen which contains a single textbox and a Search button.
* The user can type in the textbox any string which is part of any the name of an meal or
an ingedient for a meal and subsequently press the Search button to display ALL the
meals in the database which contain the typed string in the Name or Ingredients fields of
the meal in the corresponding table containing this information.
* The search should be case insensitive and a match does not have to be a complete match
but partial. I.e. if the user types the string “pEp” the displayed meal(s) could be, a
meal which contains “Roasted Peppers” or “red pepper” in the list of meals (names or
ingredients of meals).
* Extend the application so that when the user clicks on the Search for Meals button (in
the last subquestion) all the meals displayed are also displaying their image (it could be a
small thumbnail image)
* For all the tasks, the application should behave in a user friendly manner when the device
is rotated from portrait to landscape and back to portrait mode. I.e. the application
should resume from exactly the same point (same screen and data) when the orientation
changes. The rotation of the device should not change what was the user was seeing before
the rotation.
*  Extend the application so that the initial screen contains an additional button which the
user can press and subsequently type a string. All meals containing the string as part of
their name will be retrieved directly from the Web service (NOT THE DATABASE)
and displayed to the user.

## Technologies

![Static Badge](https://img.shields.io/badge/Kotlin-%237F52FF?style=for-the-badge&logo=Kotlin&logoColor=white)  
![Static Badge](https://img.shields.io/badge/Android%20Studio-%233DDC84?style=for-the-badge&logo=Android%20Studio&logoColor=white)

## Implementation

### Code Snippets

There were several aspects of developing programs that I had not yet tackled during my studies prior to this project, this included the use of APIs to retrieve data, formatting that data using JSON 
as well as utilising a database to store data, in this case Kotlin's built in Room database functionality.

Below shows the code snippet for how I opened a connection to the API and retrieved the appropriate data to be used in the application.

```kotlin
runBlocking {
   withContext(Dispatchers.IO) {
      // Resets certain data on each run through (when search is pressed)
      stringBuilder = StringBuilder("")
      mealData.clear()

      // Creates URL from the URL string and opens a connection
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
```

This next section shows the process I used for parsing the JSON data, this proved to be a challenging aspect of the project for me as I did not have experience with this at the time.

```kotlin
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
```

### App Screenshots

Although the User Interface itself was not a focus of the project, I have included some screenshots to demonstrate the look and functionality.

<p float="left">
  <img src="https://github.com/SamC95/Kotlin_MealDatabase/assets/132593571/9836dd17-30f7-47b0-91cf-31f7d09eacd8" width="250" />
  <img src="https://github.com/SamC95/Kotlin_MealDatabase/assets/132593571/4fe097ea-ae9b-4340-b7ca-5c03b2a7e600" width="250" />
  <img src="https://github.com/SamC95/Kotlin_MealDatabase/assets/132593571/f92947bb-f1ec-498c-be8c-fcaa67c4a3d7" width="250" />
  <img src="https://github.com/SamC95/Kotlin_MealDatabase/assets/132593571/ea7a9a8a-800d-4548-afc6-99eb5f92b875" width="250" />
  <img src="https://github.com/SamC95/Kotlin_MealDatabase/assets/132593571/2109b694-f5cc-4eca-a34a-a8ba34e2f538" width="250" />
</p>

## Key Learnings
* Gained further knowledge of Kotlin development.
* Utilised the Room Library database storage for the first time.
* First time utilising an API in a project.
* This was also my first time using JSON and parsing data received in that format.

## Achievements
* Implemented all functionality laid out in the project brief with no bugs or unintended behaviour to my knowledge.
* Achieved full marks for the project.

## Challenges
* I had some challenges figuring out how to parse the JSON data, I feel that my solution was likely not as efficient as it could be. In future Kotlin projects I would aim to make this a focus so I can improve upon this aspect.

## Conclusions
Overall, I feel this project allowed me to further enhance my skills with Kotlin, as well as gaining new knowledge by utilising APIs, JSON and the Room Library. If I were to improve upon the application then I would likely aim to
make the look and feel of the application better; as the user interface was not a focus of the project beyond ensuring that the appropriate elements displayed and the state was retained when changing orientation.
