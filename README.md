# Android Compass

Android compass application that contains an animated compass along with the current device coordinates.
It requires the following sensors:
- Accelerometer, used to isolate the force of gravity on all three axes by applying a low-pass filter on the accelerometer's readings.
- Magnetometer, used to get the readings for the device's magnetic environment for all three axes
- GPS, used to get longitude and latitude.
I calculate the device's tilt angle and rotation matrix by converting a vector from the device's coordinates system to real world coordinates system.
Keep in mind that
  RotationMatrix*Gravity = magnitude of gravity 
  and 
  TiltAngle*RotationMatrix*Geomagnetism = magnitude of geomagnetic field
Then, I use the Rotation Matrix to get the device's Orientation and then get the Azimuth which is used to create the compass' arrow animation.

Sensor Sampling rate = 20.000 μs = 0.02 s

The application has the added functionality of writing visited coordinates to a log file (no duplicates).

Besides the aforementioned sensors, it requires minimum Android version 6.0 (API Level 23).
Permissions:
- ACCESS_FINE_LOCATION
- WRITE_EXTERNAL_STORAGE
- INTERNET

![Screenshot](/Screenshots/app1.PNG)


![Screenshot](/Screenshots/app2.PNG)
