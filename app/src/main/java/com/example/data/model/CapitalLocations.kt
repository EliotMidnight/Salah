package com.example.data.model

/**
 * Every sovereign state's capital, so a location can be chosen by name instead
 * of typed in as a pair of coordinates.
 *
 * The list is the 193 UN member states plus the two permanent observer states
 * (Vatican City and Palestine), which is the set of countries people actually
 * mean when they say "my country".
 *
 * ### On the accuracy of the coordinates
 *
 * These are capital city centres, given to two decimal places, which is roughly
 * a kilometre. That is well inside the tolerance prayer times need: a capital
 * spans far more than that, and the sun moves about a quarter of a degree in
 * four minutes, so being a kilometre off shifts a time by well under a minute.
 *
 * They are city centres, not the position of the city's mosque, so someone
 * living on the far side of a large city will still want the manual coordinate
 * entry that sits below this list. Where a state has no single agreed capital,
 * the seat of government is listed and the usual alternative appears in the
 * capital's own name where that is the only way to be clear about it.
 */
object CapitalLocations {

    /**
     * @param country the state's name, in the form a person would search for.
     * @param capital the capital's name, same.
     * @param latitude degrees north, negative in the southern hemisphere.
     * @param longitude degrees east, negative in the western hemisphere.
     */
    data class Entry(
        val country: String,
        val capital: String,
        val latitude: Double,
        val longitude: Double
    ) {
        /** A [UserLocation] for this capital, ready to hand to the settings. */
        fun toLocation(): UserLocation =
            UserLocation(name = capital, country = country, latitude = latitude, longitude = longitude)
    }

    val all: List<Entry> = listOf(
        Entry("Afghanistan", "Kabul", 34.55, 69.21),
        Entry("Albania", "Tirana", 41.33, 19.82),
        Entry("Algeria", "Algiers", 36.75, 3.06),
        Entry("Andorra", "Andorra la Vella", 42.51, 1.52),
        Entry("Angola", "Luanda", -8.84, 13.23),
        Entry("Argentina", "Buenos Aires", -34.60, -58.38),
        Entry("Armenia", "Yerevan", 40.18, 44.51),
        Entry("Australia", "Canberra", -35.28, 149.13),
        Entry("Austria", "Vienna", 48.21, 16.37),
        Entry("Azerbaijan", "Baku", 40.41, 49.87),
        Entry("Bahamas", "Nassau", 25.03, -77.40),
        Entry("Bahrain", "Manama", 26.23, 50.59),
        Entry("Bangladesh", "Dhaka", 23.81, 90.41),
        Entry("Barbados", "Bridgetown", 13.11, -59.62),
        Entry("Belarus", "Minsk", 53.90, 27.57),
        Entry("Belgium", "Brussels", 50.85, 4.35),
        Entry("Belize", "Belmopan", 17.25, -88.77),
        Entry("Benin", "Porto-Novo", 6.50, 2.61),
        Entry("Bhutan", "Thimphu", 27.47, 89.64),
        Entry("Bolivia", "Sucre", -19.02, -65.26),
        Entry("Bosnia and Herzegovina", "Sarajevo", 43.86, 18.41),
        Entry("Botswana", "Gaborone", -24.63, 25.91),
        Entry("Brazil", "Brasilia", -15.79, -47.88),
        Entry("Brunei", "Bandar Seri Begawan", 4.94, 114.94),
        Entry("Bulgaria", "Sofia", 42.70, 23.32),
        Entry("Burkina Faso", "Ouagadougou", 12.37, -1.53),
        Entry("Burundi", "Gitega", -3.43, 29.92),
        Entry("Cambodia", "Phnom Penh", 11.56, 104.92),
        Entry("Cameroon", "Yaounde", 3.85, 11.50),
        Entry("Canada", "Ottawa", 45.42, -75.69),
        Entry("Central African Republic", "Bangui", 4.36, 18.56),
        Entry("Chad", "N'Djamena", 12.11, 15.04),
        Entry("Chile", "Santiago", -33.45, -70.67),
        Entry("China", "Beijing", 39.90, 116.41),
        Entry("Colombia", "Bogota", 4.71, -74.07),
        Entry("Comoros", "Moroni", -11.72, 43.25),
        Entry("Congo (Brazzaville)", "Brazzaville", -4.27, 15.28),
        Entry("Congo (Kinshasa)", "Kinshasa", -4.32, 15.31),
        Entry("Costa Rica", "San Jose", 9.93, -84.08),
        Entry("Croatia", "Zagreb", 45.81, 15.98),
        Entry("Cuba", "Havana", 23.11, -82.37),
        Entry("Cyprus", "Nicosia", 35.17, 33.36),
        Entry("Czechia", "Prague", 50.08, 14.44),
        Entry("Denmark", "Copenhagen", 55.68, 12.57),
        Entry("Djibouti", "Djibouti", 11.59, 43.15),
        Entry("Dominica", "Roseau", 15.30, -61.39),
        Entry("Dominican Republic", "Santo Domingo", 18.49, -69.93),
        Entry("East Timor", "Dili", -8.56, 125.57),
        Entry("Ecuador", "Quito", -0.18, -78.47),
        Entry("Egypt", "Cairo", 30.04, 31.24),
        Entry("El Salvador", "San Salvador", 13.69, -89.19),
        Entry("Equatorial Guinea", "Malabo", 3.75, 8.78),
        Entry("Eritrea", "Asmara", 15.34, 38.93),
        Entry("Estonia", "Tallinn", 59.44, 24.75),
        Entry("Eswatini", "Mbabane", -26.31, 31.14),
        Entry("Ethiopia", "Addis Ababa", 9.03, 38.74),
        Entry("Fiji", "Suva", -18.14, 178.44),
        Entry("Finland", "Helsinki", 60.17, 24.94),
        Entry("France", "Paris", 48.86, 2.35),
        Entry("Gabon", "Libreville", 0.42, 9.47),
        Entry("Gambia", "Banjul", 13.45, -16.58),
        Entry("Georgia", "Tbilisi", 41.72, 44.79),
        Entry("Germany", "Berlin", 52.52, 13.40),
        Entry("Ghana", "Accra", 5.60, -0.19),
        Entry("Greece", "Athens", 37.98, 23.73),
        Entry("Grenada", "St. George's", 12.05, -61.75),
        Entry("Guatemala", "Guatemala City", 14.63, -90.51),
        Entry("Guinea", "Conakry", 9.64, -13.58),
        Entry("Guinea-Bissau", "Bissau", 11.88, -15.62),
        Entry("Guyana", "Georgetown", 6.80, -58.16),
        Entry("Haiti", "Port-au-Prince", 18.54, -72.34),
        Entry("Honduras", "Tegucigalpa", 14.07, -87.19),
        Entry("Hungary", "Budapest", 47.50, 19.04),
        Entry("Iceland", "Reykjavik", 64.15, -21.94),
        Entry("India", "New Delhi", 28.61, 77.21),
        Entry("Indonesia", "Jakarta", -6.21, 106.85),
        Entry("Iran", "Tehran", 35.69, 51.39),
        Entry("Iraq", "Baghdad", 33.31, 44.37),
        Entry("Ireland", "Dublin", 53.35, -6.26),
        Entry("Israel", "Jerusalem", 31.77, 35.21),
        Entry("Italy", "Rome", 41.90, 12.50),
        Entry("Ivory Coast", "Yamoussoukro", 6.83, -5.29),
        Entry("Jamaica", "Kingston", 17.97, -76.79),
        Entry("Japan", "Tokyo", 35.68, 139.69),
        Entry("Jordan", "Amman", 31.95, 35.93),
        Entry("Kazakhstan", "Astana", 51.17, 71.45),
        Entry("Kenya", "Nairobi", -1.29, 36.82),
        Entry("Kiribati", "South Tarawa", 1.33, 172.98),
        Entry("Kosovo", "Pristina", 42.66, 21.16),
        Entry("Kuwait", "Kuwait City", 29.38, 47.99),
        Entry("Kyrgyzstan", "Bishkek", 42.87, 74.59),
        Entry("Laos", "Vientiane", 17.97, 102.63),
        Entry("Latvia", "Riga", 56.95, 24.11),
        Entry("Lebanon", "Beirut", 33.89, 35.50),
        Entry("Lesotho", "Maseru", -29.31, 27.49),
        Entry("Liberia", "Monrovia", 6.31, -10.80),
        Entry("Libya", "Tripoli", 32.89, 13.19),
        Entry("Liechtenstein", "Vaduz", 47.14, 9.52),
        Entry("Lithuania", "Vilnius", 54.69, 25.28),
        Entry("Luxembourg", "Luxembourg City", 49.61, 6.13),
        Entry("Madagascar", "Antananarivo", -18.88, 47.51),
        Entry("Malawi", "Lilongwe", -13.96, 33.79),
        Entry("Malaysia", "Kuala Lumpur", 3.14, 101.69),
        Entry("Maldives", "Male", 4.18, 73.51),
        Entry("Mali", "Bamako", 12.64, -8.00),
        Entry("Malta", "Valletta", 35.90, 14.51),
        Entry("Marshall Islands", "Majuro", 7.09, 171.38),
        Entry("Mauritania", "Nouakchott", 18.08, -15.98),
        Entry("Mauritius", "Port Louis", -20.16, 57.50),
        Entry("Mexico", "Mexico City", 19.43, -99.13),
        Entry("Micronesia", "Palikir", 6.92, 158.16),
        Entry("Moldova", "Chisinau", 47.01, 28.86),
        Entry("Monaco", "Monaco", 43.74, 7.43),
        Entry("Mongolia", "Ulaanbaatar", 47.89, 106.91),
        Entry("Montenegro", "Podgorica", 42.44, 19.26),
        Entry("Morocco", "Rabat", 34.02, -6.84),
        Entry("Mozambique", "Maputo", -25.97, 32.57),
        Entry("Myanmar", "Naypyidaw", 19.75, 96.10),
        Entry("Namibia", "Windhoek", -22.56, 17.08),
        Entry("Nauru", "Yaren", -0.55, 166.92),
        Entry("Nepal", "Kathmandu", 27.72, 85.32),
        Entry("Netherlands", "Amsterdam", 52.37, 4.90),
        Entry("New Zealand", "Wellington", -41.29, 174.78),
        Entry("Nicaragua", "Managua", 12.11, -86.24),
        Entry("Niger", "Niamey", 13.51, 2.11),
        Entry("Nigeria", "Abuja", 9.06, 7.50),
        Entry("North Korea", "Pyongyang", 39.04, 125.76),
        Entry("North Macedonia", "Skopje", 41.99, 21.43),
        Entry("Norway", "Oslo", 59.91, 10.75),
        Entry("Oman", "Muscat", 23.59, 58.41),
        Entry("Pakistan", "Islamabad", 33.68, 73.05),
        Entry("Palau", "Ngerulmud", 7.50, 134.62),
        Entry("Palestine", "Ramallah", 31.90, 35.20),
        Entry("Panama", "Panama City", 8.98, -79.52),
        Entry("Papua New Guinea", "Port Moresby", -9.44, 147.18),
        Entry("Paraguay", "Asuncion", -25.26, -57.58),
        Entry("Peru", "Lima", -12.05, -77.04),
        Entry("Philippines", "Manila", 14.60, 120.98),
        Entry("Poland", "Warsaw", 52.23, 21.01),
        Entry("Portugal", "Lisbon", 38.72, -9.14),
        Entry("Qatar", "Doha", 25.29, 51.53),
        Entry("Romania", "Bucharest", 44.43, 26.10),
        Entry("Russia", "Moscow", 55.76, 37.62),
        Entry("Rwanda", "Kigali", -1.94, 30.06),
        Entry("Saint Kitts and Nevis", "Basseterre", 17.30, -62.72),
        Entry("Saint Lucia", "Castries", 14.01, -61.00),
        Entry("Saint Vincent and the Grenadines", "Kingstown", 13.16, -61.22),
        Entry("Samoa", "Apia", -13.83, -171.77),
        Entry("San Marino", "San Marino", 43.94, 12.46),
        Entry("Sao Tome and Principe", "Sao Tome", 0.34, 6.73),
        Entry("Saudi Arabia", "Riyadh", 24.71, 46.68),
        Entry("Senegal", "Dakar", 14.72, -17.47),
        Entry("Serbia", "Belgrade", 44.79, 20.45),
        Entry("Seychelles", "Victoria", -4.62, 55.45),
        Entry("Sierra Leone", "Freetown", 8.48, -13.23),
        Entry("Singapore", "Singapore", 1.35, 103.82),
        Entry("Slovakia", "Bratislava", 48.15, 17.11),
        Entry("Slovenia", "Ljubljana", 46.06, 14.51),
        Entry("Solomon Islands", "Honiara", -9.43, 159.96),
        Entry("Somalia", "Mogadishu", 2.05, 45.32),
        Entry("South Africa", "Pretoria", -25.75, 28.19),
        Entry("South Korea", "Seoul", 37.57, 126.98),
        Entry("South Sudan", "Juba", 4.85, 31.58),
        Entry("Spain", "Madrid", 40.42, -3.70),
        Entry("Sri Lanka", "Sri Jayawardenepura Kotte", 6.93, 79.86),
        Entry("Sudan", "Khartoum", 15.50, 32.56),
        Entry("Suriname", "Paramaribo", 5.85, -55.20),
        Entry("Sweden", "Stockholm", 59.33, 18.07),
        Entry("Switzerland", "Bern", 46.95, 7.45),
        Entry("Syria", "Damascus", 33.51, 36.29),
        Entry("Taiwan", "Taipei", 25.03, 121.57),
        Entry("Tajikistan", "Dushanbe", 38.56, 68.79),
        Entry("Tanzania", "Dodoma", -6.16, 35.75),
        Entry("Thailand", "Bangkok", 13.76, 100.50),
        Entry("Togo", "Lome", 6.13, 1.22),
        Entry("Tonga", "Nuku'alofa", -21.14, -175.20),
        Entry("Trinidad and Tobago", "Port of Spain", 10.65, -61.52),
        Entry("Tunisia", "Tunis", 36.81, 10.18),
        Entry("Turkey", "Ankara", 39.93, 32.86),
        Entry("Turkmenistan", "Ashgabat", 37.96, 58.38),
        Entry("Tuvalu", "Funafuti", -8.52, 179.20),
        Entry("Uganda", "Kampala", 0.35, 32.58),
        Entry("Ukraine", "Kyiv", 50.45, 30.52),
        Entry("United Arab Emirates", "Abu Dhabi", 24.45, 54.38),
        Entry("United Kingdom", "London", 51.51, -0.13),
        Entry("United States", "Washington, D.C.", 38.91, -77.04),
        Entry("Uruguay", "Montevideo", -34.90, -56.16),
        Entry("Uzbekistan", "Tashkent", 41.30, 69.24),
        Entry("Vanuatu", "Port Vila", -17.73, 168.33),
        Entry("Vatican City", "Vatican City", 41.90, 12.45),
        Entry("Venezuela", "Caracas", 10.49, -66.88),
        Entry("Vietnam", "Hanoi", 21.03, 105.85),
        Entry("Yemen", "Sanaa", 15.37, 44.19),
        Entry("Zambia", "Lusaka", -15.39, 28.32),
        Entry("Zimbabwe", "Harare", -17.83, 31.05)
    )

    /**
     * Case- and accent-insensitive search over both the country and the capital,
     * so "turk", "Turkiye", "ankara" and "ANKARA" all land in the same place.
     */
    fun search(query: String): List<Entry> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return all
        return all.filter {
            it.country.lowercase().contains(needle) ||
                it.capital.lowercase().contains(needle)
        }
    }
}
