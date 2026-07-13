The CreateTreeAnalysisPage is a page composable, it is responsible for creating a new TreeAnalysis Record
When User route into this page, this means that they what to create a new TreeAnalysis Record

The main content of CreateTreeAnalysisPage is a form, the form corresponds to the TreeAnalysisRecordEntity (except the autogen id)
The state of the form should be synced with a TreeAnalysisRecordEntity instance.

The following fields of TreeAnalysisRecordEntity need special logic to handle

## imageDir
The imageDir is a dir in External Storage
When enter CreateTreeAnalysisPage, a new dir name should be generated according to current time to ensure uniqueness of dir name
By convention, this folder contains a file named leaf.png. At the time of creation, however, this file does not yet exist.
The form must provide an image picker that corresponds to leaf.png. When the user selects an image through the form, the selected image shall be saved as leaf.png.
If leaf.png already exists, the form shall display a preview of the current image. The user must be able to replace the existing image with a new selection.


## extra
extra is a JSON string of TreeAnalysisRecordExtra.kt
the form should contain item for the fields in TreeAnalysisRecordExtra
The state of this TreeAnalysisRecordExtra fields should be synced with a TreeAnalysisRecordExtra instance
when the TreeAnalysisRecordEntity instance is to be submitted, the latest state of TreeAnalysisRecordExtra instance shoudld be transformed into a JSON string and set to the extra field of TreeAnalysisRecordEntity instance

##  createdAt and  deletedAt 
This two fields should be ignored by the form