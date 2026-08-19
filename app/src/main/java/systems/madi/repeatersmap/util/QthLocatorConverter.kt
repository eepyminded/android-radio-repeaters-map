package systems.madi.repeatersmap.util

object QthLocatorConverter {

    // convert maidenhead coordinates string into latitude longitude list
    fun convertToCoordinates(locator: String?): List<Double>? {
        if (locator.isNullOrBlank()) return null
        
        val cleanLocator = locator.trim().uppercase()
        
        // locators must be an even number of characters, up to 6 (we support 2, 4, 6)
        if (cleanLocator.length % 2 != 0 || cleanLocator.isEmpty()) return null

        // start at the bottom-left of the world grid
        var longitude = -180.0
        var latitude = -90.0

        // track the size of the current bounding box
        var lonWidth = 360.0
        var latHeight = 180.0

        try {
            // pair 1: field (letters A-R, 20 deg lon x 10 deg lat)
            if (cleanLocator.length >= 2) {
                val lonChar = cleanLocator[0]
                val latChar = cleanLocator[1]
                
                if (lonChar !in 'A'..'R' || latChar !in 'A'..'R') return null
                
                lonWidth = 20.0
                latHeight = 10.0
                
                longitude += (lonChar - 'A') * lonWidth
                latitude += (latChar - 'A') * latHeight
            }

            // pair 2: square (numbers 0-9, 2 deg lon x 1 deg lat)
            if (cleanLocator.length >= 4) {
                val lonChar = cleanLocator[2]
                val latChar = cleanLocator[3]
                
                if (lonChar !in '0'..'9' || latChar !in '0'..'9') return null
                
                lonWidth = 2.0
                latHeight = 1.0
                
                longitude += (lonChar - '0') * lonWidth
                latitude += (latChar - '0') * latHeight
            }

            // pair 3: subsquare (letters A-X, 5 min lon x 2.5 min lat)
            if (cleanLocator.length >= 6) {
                val lonChar = cleanLocator[4]
                val latChar = cleanLocator[5]
                
                if (lonChar !in 'A'..'X' || latChar !in 'A'..'X') return null
                
                lonWidth = 5.0 / 60.0
                latHeight = 2.5 / 60.0
                
                longitude += (lonChar - 'A') * lonWidth
                latitude += (latChar - 'A') * latHeight
            }

            // find the center of the final bounding box
            val centerLongitude = longitude + (lonWidth / 2.0)
            val centerLatitude = latitude + (latHeight / 2.0)

            // app schema expects [latitude, longitude]
            return listOf(centerLatitude, centerLongitude)

        } catch (e: Exception) {
            return null
        }
    }
}
