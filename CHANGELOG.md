# Changelog

## Version 0.1.53.1-SNAPSHOT
### New Features

Support of hierarchic menus & page structures: 
1. on configuration scope an additional parameter {:page-model [:flat | :hierarchic]} - flat will be the default. Flat mode is backward compatible.
2. on page scope we respect {:navbar? [true|false]} in flat & hierarchic mode also.
3. params now contain hierarchic pages. Pages is replacing no longer supported navbar-pages / sidebar-pages. Hierarchic pages may contain a sequence of children `{:children ({:title "child-page", :layout "page.html", :content " <p>child</p>"}) }

### Breaking Changes
Pages is replacing no longer supported navbar-pages / sidebar-pages. In order to realice navbar / sidebar functionality, you've now to write filters.
