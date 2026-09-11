# Binge List — OPSC6312 POE

## Group 17:
- ST10441936 - Sibongile Zandile Nhlapo(Sign Up screen)
- ST10447508 - Natasha Mvundlela(WatchList Screen)
- ST10349188 - Molebogeng Eddiemon Thupi(Settings screen)
- ST10445772 - Ronewa Nemubvumoni(Tester and User Interface Designer)
- ST10440580 - Tshepang Ramohapi(Api integration, Favorite screen and discover screen)

## What this app does

Binge List is an Android movie discovery and watchlist application that helps users discover movies and keep track of what they want to watch. Users can search for movies, browse movies by genre, add movies to their watchlist and favorites, and manage their preferences through the application.

The app provides a simple and visually appealing interface designed to make discovering and organizing movies quick and easy.

## Features implemented in Formative 02 Part 02

-  Register / log in (password encrypted)
-  Settings screen
-  REST API connected to a hosted database
-  Movie discovery and browsing
-  Search movies by title or genre
-  Browse movies by genre
-  Add movies to Watchlist
-  Add movies to Favorites
-  Discover screen with "New This Week" movies
-  Bottom navigation between Discover, Watchlist, Favorites and Settings

## Tech stack

- Android Studio, Kotlin, XML Views / ViewBinding
- RecyclerView for movie and genre lists
- Retrofit for REST API communication
- Material Components for Android UI components
- ConstraintLayout and NestedScrollView for screen layouts
- REST API + hosted database
- Room / local storage(Might use this)

## API & hosting

- API base URL for OmdbApi: https://www.omdbapi.com/
- API base URL for TmdbApi: https://api.themoviedb.org/3/
- Database: SQL and Firebase

## Changelog v1

### Added

- Added Binge List Discover screen
- Added Binge List application branding and header
- Added movie search functionality
- Added "New This Week" movie section
- Added movie genre filtering
- Added Watchlist functionality
- Added Favorites functionality
- Added Settings screen
- Added bottom navigation
- Added movie RecyclerViews
- Added genre filter chips
- Added REST API integration
- Added connection to the hosted database
- Added loading and status messages for API operations
- Added responsive movie browsing interface

### Updated

- Updated the Discover screen UI to match the Binge List design
- Updated application colors and backgrounds
- Updated search bar styling
- Updated movie cards and featured movie section
- Updated genre chips to use rounded pill-shaped buttons
- Updated bottom navigation with icons and labels
- Updated spacing, typography and rounded corners throughout the application
- Updated error and loading states for improved user experience

## Running the project

1. Clone this repo
2. Open the project in Android Studio
3. Allow Android Studio to sync the Gradle dependencies
4. Add your local `local.properties` entries (see `local.properties.example` if provided)
5. Add the required API base URL and configuration
6. Make sure the hosted API and database are available
7. Run the project on an Android emulator or physical device

## Testing 

## AI tool usage

ChatGPT was used as an AI assistance tool during development of the application.

AI assistance was used for:
- Helping structure and improve Android XML layouts
- Troubleshooting Android layout and rendering errors
- Helping diagnose `InvocationTargetException` and font resource errors

All AI-generated suggestions were reviewed, adapted and integrated by the development team where appropriate.
AI tools were used as an aid during development and did not replace the team's responsibility for understanding, testing and implementing the application's functionality.
