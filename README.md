<img src="images/Logo.svg" alt="Logo" height="120">
  

<!-- ABOUT THE PROJECT -->
# About CovidApp

CovidApp - Covid Community Alert is an open-source contact tracing app that uses Bluetooth LE to anonimously track interactions between devices.  

This app goes in pair with [CoviDoc](https://gitlab.com/coronavirus-outbreak-control/coronavirus-outbreak-control-android-doctors),
the app that doctors can use to identify infected patients.   

### Privacy
Our aim is to protect and maintain the privacy of our users - we **do not take any personal information, or store continuos real-time GPS location**
of the users;  

If the user gives consent of sharing the location (which is **optional**), we do not actively keep track of it:
we only add the location to the interactions, so if interactions are not occuring no data is being logged at all.
When a location is recorded it is *intentionally* with a low precision (100-120 meters); this is to make it impossible to know the exact location of a user,
but still allow insight into which areas could see an increase in cases.


### Built With
* [Android Studio](https://developer.android.com/studio)
* [Java](https://www.java.com/en/download/)
* [Android Beacon Library](https://altbeacon.github.io/android-beacon-library/)



<!-- GETTING STARTED -->
## Getting Started
Just clone the repo and open it with Android Studio!



<!-- ROADMAP -->
### Roadmap

See the [open Issues](https://gitlab.com/coronavirus-outbreak-control/coronavirus-outbreak-control-android/-/issues) for a list of proposed features (and known issues).
If you find any issues we are not aware of in the app, please **open an issue**! We will really appreciate your contributions.


<!-- CONTRIBUTING -->
### Contributing

If you believe in this project and in the importance of privacy of the user, just like we do, please contribute. It's easy:

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Merge Request. We'll review it and merge it!



<!-- LICENSE -->
## License

Distributed under the Apache 2.0 License. See `LICENSE` for more information.



<!-- CONTACT -->
## Contact
Elena Martina - Android Lead - [LinkedIn](https://www.linkedin.com/in/elena-martina/) - elenamartina98@gmail.com

Coronavirus Outbreak Control - [Website](https://coronavirus-outbreak-control.github.io/web/index.html) - coronavirus.outbreak.control@gmail.com






