# CameraPoseCapture - Capture Photos with Camera Pose

## Project Description
**CameraPoseCapture** is an Android application developed using Java for capturing photos and calculating the camera pose for each shot. It integrates a Python environment to implement a recommendation system, which suggests optimal positions for taking additional photos. Finally, the captured photos are stitched together into a partial panorama.

## Features
 - Capture high-resolution photos with precise camera pose calculation.
 - Recommend shooting locations to improve coverage, powered by a Python-based recommendation system.
 - Stitch captured photos into a seamless partial panorama.
 - User-friendly interface for smooth and efficient usage.


## Technical Stack
### Programming Languages:
 - Java: Core application development.
 - Python: Recommendation system integration.
### Frameworks & Libraries:
 - Android SDK: Core Android development.
 - OpenCV: Image processing and panorama stitching.
 - Chaquopy: Embedding Python in the Android app.
### Tools:
 - Gradle: Build and dependency management.
 - Git: Version control.

## How It Works
1. Photo Capture and Camera Pose Calculation:
    - The app captures photos and calculates the camera pose (e.g., position and orientation) for each shot.
2. Recommendation System:
    - A Python-based algorithm analyzes captured photos and suggests positions for additional shots to ensure better coverage.
3. Photo Pasting:
    - Using OpenCV, the app stitches captured photos into a partial panorama for visualization.


