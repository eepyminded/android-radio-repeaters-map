# ⚠️ EARLY WIP! ⚠️

# 🗺️ Android Radio Repeaters Map

<div align="center">

**An interactive Android Compose map showing amateur radio repeaters**

</div>

## 📖 Overview

The Android Radio Repeaters Map is a mobile application built for Polish amateur radio enthusiasts and operators. It provides an interactive map where you can zoom around, find your local repeaters and check their details to access them.

## ✨ Features

-   🎯 **Interactive Map Display:** Visualize radio repeater locations on a dynamic and zoomable map
-   📍 **Repeater Location Markers:** Clearly marked points on the map representing each radio repeater
-   ℹ️ **Detailed Repeater Information:** Access details like RX/TX frequency, call sign, tone, and status upon clicking on repeater marker
-   🔍 **Search & Filter Capabilities:** Filter repeaters by frequency band (23cm, 70cm, 2m, 4m, 6m, 10m) and operational status
-   📍 **GPS Localization & Real-Time Location:** Automatically center on your position with smooth camera animations and live visual location

## 🔧 TODO
-   🌍❓ **QTH Converter:** Add a feature to convert QTH locators (e.g., `KO02MM`) to coordinates in entries where latitude and longitude are missing, so they don't end up in the middle of the ocean

## 🖥️ Screenshots

<p align="center">
  <img src="https://raw.githubusercontent.com/eepyminded/android-radio-repeaters-map/refs/heads/main/images/Wroclaw.png" width="30%" />
  <img src="https://raw.githubusercontent.com/eepyminded/android-radio-repeaters-map/refs/heads/main/images/details.png" width="30%" />
  <img src="https://raw.githubusercontent.com/eepyminded/android-radio-repeaters-map/refs/heads/main/images/country.png" width="30%" />
</p>

## 🤝 Credits
- MapLibre library, specifically demos section for compose wrapper - greatly helped with making this project
- To Wojtek Jakieła SQ8W from przemienniki.eu, for .json file containing all radio repeaters information for users to use
