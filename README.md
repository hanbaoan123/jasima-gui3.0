# jasima GUI #

jasima GUI is an Eclipse plugin that can be used to create, configure and run jasima experiments. jasima is the JAva SImulator for MAnufacturing and logistics, its homepage can be found at http://jasima.net.

To simply install and run jasima GUI, please follow the instructions on http://jasima.net. The information below is only relevant for you, if you want to access/modify the source code of the Eclipse plugin.

### Import ###

The repository contains three Eclipse projects. Most importantly in the sub-directory `jasima_gui` the source code of the plugin itself is located. `jasima_gui_feature` and `jasima_gui_site` are support projects to create an Eclipse update site for jasima GUI. When you clone the repository using Eclipse's EGit make sure to check the option "Import all existing Eclipse projects after clone finished" in the wizard. After finishing the import Eclipse should have created three new projects: `jasima_gui`, `jasima_gui_feature`, `jasima_gui_site`.

### How do I get set up? ###

explain dependencies

### Contact ###

Torsten Hildebrandt <torsten@jasima.net>