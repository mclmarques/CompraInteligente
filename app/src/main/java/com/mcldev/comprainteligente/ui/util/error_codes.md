# Error Code Reference

This file contains the list of error codes used in the application and their corresponding descriptions.

| Code | Description                                      | String Resource Name                                 |
|------|--------------------------------------------------|------------------------------------------------------|
| 1    | Error launching the camera                       | `err_launch_camera_title` & `err_launch_camera_title` |
| 2    | Error extracting the text from the image         | `err_text_extraction` & `err_text_extraction_title`  |
| 3    | Error saving the data                            | `err_saving_data_title` & `err_saving_data`          |
| 4    | Error unsupported device (insufficient RAM) 1    | `err_unsupported_title` & `err_unsupported`          |
| 5    | Error unsupported device (insufficient Storge) 2 | `err_storage_title` & `err_storage`                  |
| 6    | Error connecting to Google account               | -                                                    |
| 6    | Error saving to the cloud                        | -                                                    |

## Notes
- Codes without a **String Resource** entry indicate that the description is not yet implemented. 
Hardcoding error codes should **ONLY BE DONE DURING DEVELOPMENT PHASE**

- String resources should be defined in `res/values/strings.xml` for proper localization where applicable.

- Error 4 may be removed in the future. As of 08/01/2025, in order to use the Document scanner with ML Kit on Android, 
the device needs to have at least a device total RAM of 1.7GB. Otherwise, it returns an MlKitException with error code UNSUPPORTED 
when calling the API. During the onCreate stage, the system will check for at least 2GB of RAM or show an unsupported warning message