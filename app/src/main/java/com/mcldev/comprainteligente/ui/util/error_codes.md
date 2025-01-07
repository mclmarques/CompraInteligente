# Error Code Reference

This file contains the list of error codes used in the application and their corresponding descriptions.

| Code | Description                                  | String Resource Name                                  |
|---|----------------------------------------------|------------------------------------------------------|
| 1 | Error launching the camera                  | `err_launch_camera_title` & `err_launch_camera_title` |
| 2 | Error extracting the text from the image    | `err_text_extraction` & `err_text_extraction_title`   |
| 3 | Error saving the data                       | `err_saving_data_title` & `err_saving_data`           |
| 4 | Error saving into the cloud                 | -                                                     |
| 5 | Error connecting to Google account          | -                                                     |

## Notes
- Codes without a **String Resource** entry indicate that the description is not yet implemented. Hardcoding error codes should **ONLY BE DONE ON DEVELOPMENT**
- String resources should be defined in `res/values/strings.xml` for proper localization where applicable.
