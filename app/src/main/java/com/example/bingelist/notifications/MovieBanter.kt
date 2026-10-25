package com.example.bingelist.notifications

object MovieBanter {

    data class Banter(val title: String, val body: String, val imdbId: String)

    // Each one opens the movie it jokes about when tapped.
    val ALL = listOf(
        Banter(
            "We'll be back (so will your watchlist)",
            "Your unwatched movies are standing by with serious T-800 energy. Tap to terminate the boredom.",
            "tt0088247" // The Terminator
        ),
        Banter(
            "May the Force be with your watchlist",
            "Use the Force, Luke. Or just tap here and pick a film like a normal person.",
            "tt0076759" // Star Wars
        ),
        Banter(
            "You're gonna need a bigger watchlist",
            "Something large has been spotted in your queue. Tap to take a bite out of it.",
            "tt0073195" // Jaws
        ),
        Banter(
            "An offer you can't refuse",
            "Tonight's movie is on the house. Tap to see what's waiting in your watchlist.",
            "tt0068646" // The Godfather
        ),
        Banter(
            "Red pill or blue pill?",
            "Red: open BingeList and pick a movie. Blue: scroll your phone until bedtime. Choose wisely.",
            "tt0133093" // The Matrix
        ),
        Banter(
            "Life is like a watchlist",
            "You never know what you're gonna get. Tap to find out.",
            "tt0109830" // Forrest Gump
        ),
        Banter(
            "To infinity and your watchlist!",
            "Your queue has more titles than Buzz has catchphrases. Tap to choose tonight's movie.",
            "tt0114709" // Toy Story
        ),
        Banter(
            "Great Scott! It's movie time",
            "Your watchlist is stuck in 1985. Tap to catch up on a classic.",
            "tt0088763" // Back to the Future
        ),
        Banter(
            "Just keep watching",
            "Just keep watching, just keep watching. You've got a movie waiting. Tap to dive in.",
            "tt0266543" // Finding Nemo
        ),
        Banter(
            "Are you not entertained?",
            "The crowd is restless. Tap to pick a movie from your watchlist and give them a show.",
            "tt0172495" // Gladiator
        )
    )
}