# Change Log

All notable changes to this project will be documented in this
file. This change log follows the conventions of
[keepachangelog.com](http://keepachangelog.com/), with the addition of
sections for `Highlights`, `Breaking` and `Changes since x.y.z`.

## [Unreleased]

## [0.4.0] - 2020-12-05

### Highlights
- Allow fast compilation of changed files only.
- Enable post-processing of HTML files.
- Allow the sitemap to exclude specified files.

### Fixes
- Links to tag pages are URL-encoded. Tags with special chars now work correctly.

### Changes since 0.3.2
- [#155](https://github.com/cryogen-project/cryogen-core/pull/155) Prepare for 0.4.0 (bombaywalla)
- [#154](https://github.com/cryogen-project/cryogen-core/pull/154) Simplify the contrib/release docs (holyjak)
- [#153](https://github.com/cryogen-project/cryogen-core/pull/153) Add support files (bombaywalla)
- [#152](https://github.com/cryogen-project/cryogen-core/pull/152) Enable postprocessing of article HTML (holyjak)
- [#150](https://github.com/cryogen-project/cryogen-core/pull/150) Allow certain paths to be excluded from the sitemap (seancorfield)
- [#149](https://github.com/cryogen-project/cryogen-core/pull/149) Feature: Incremental compilation (changed only) (holyjak)
- [#148](https://github.com/cryogen-project/cryogen-core/pull/148) Include :page-meta in config passed to markdown render (holyjak)
- [#147](https://github.com/cryogen-project/cryogen-core/pull/147) Markup: add docstrings (holyjak)
- [#143](https://github.com/cryogen-project/cryogen-core/pull/143) Fix 142: URL encode tag links (gamlerhart)

## [0.3.2] - 2020-10-16

### Highlights
- Allow the multiple file extensions for a particular markup.
- Fixed path bugs on Windows.

### Breaking
- Requires latest versions of `cryogen-asciidoc` (at least 0.3.2),
  `cryogen-flexmark` (at least 0.1.4), and `cryogen-markdown` (at
  least 0.1.13).

### Changes since 0.3.1
- [#146](https://github.com/cryogen-project/cryogen-core/pull/146) Allow multiple extensions in Markup (bombaywalla)
- [#145](https://github.com/cryogen-project/cryogen-core/pull/145) Hawk library fails in macOS Big Sur Preview (sankara)
- [#144](https://github.com/cryogen-project/cryogen-core/pull/144) Bump selmer to fix Windows path issue (jarmond)
- [#140](https://github.com/cryogen-project/cryogen-core/pull/140) Fixed resource path bug on Windows. (cpmcdaniel)

## [0.3.1] - 2020-01-20

### Highlights
- Bump to 0.3.1

## [0.3.0] - 2020-12-31

### Highlights
- Bump to version 0.3.0

## [0.2.3] - 2019-12-10

### Highlights
- Add support for `:update-article-fn` to customize parsed articles.
- See [Customizing Cryogen](http://cryogenweb.org/docs/customizing-cryogen.html) for details.
- Also added ability to customize the class on generated TOCs.

## [0.2.2] - 2019-12-05

### Highlights
- Expose plaintext description to pages/posts so it can be included in description tags for previews/SEO.
- `description` either comes from the post/page metadata or it's generated from the preview.
- Enable users to derive extra params from params + site data by passing in a `:extend-params-fn` along with overrides.

## [0.2.1] - 2019-06-15

### Highlights
- Relax config schema as many keys aren't actually required.
- (closes #118) relax config schema.

## [0.2.0] - 2019-06-15

### Highlights
- Updated to use and produce a shallower project structure.

### Changes

- Remove resources/templates folder altogether.
- Move posts and pages to content/ in root folder.
- Move `config.edn` to content/ in root folder.
- Move themes to root folder.
- Move output folder public to root folder (specifiable via `:public-dest`).
- All user-specified resource paths are with respect to content/
  folder (theme-specific resources will be relative to that theme's
  folder).
- See #117.

## [0.1.68] - 2019-06-05

## [Highlights
- Update behaviour] of clean-urls? feature.

## [Changes
- Due to] feedback in #115, this feature has been updated to fit more use cases.
- key has been changed to `:clean-urls`.  Will accept one of the following values: `:dirty` `:trailing-slash` `:no-trailing-slash`.
- See the configuration docs for more details.

## [0.1.67] - 2019-05-05

### Highlights
- Update dependencies.

### Changes
- Includes a fix to clj-rss to remove extra newlines yogthos/clj-rss#16

## [0.1.66] - 2019-03-05

## [Highlights
- Update dependencies].

## [0.1.65] - 2019-01-03

### Highlights
- Update behaviour of clean-urls? feature.

### Changes
- Files no longer generated as `/foo-bar/index.html`, generated as just `foo-bar.html` instead.
- With clean-urls? on, all page links will have trailing `.html`
  stripped.
  
## [0.1.64] - 2018-11-06

### Highlights
- Renamed `:servlet-context` to `:selmer/context`.

## [0.1.62] - 2018-09-07

### Highlights
- Fix broken sass compilation.

### Changes
- Sass compilation was broken due to Cryogen relying on the `--update` flag which was provided by Ruby Sass but not Dart Sass.
- The maintainers of Dart Sass have since added the feature and a small change in Cryogen was required to conform to their syntax.

## [0.1.61] - 2018-02-22

### Highlights
- Bump to release version.

## [0.1.60] - 2017-11-28

### Highlights
- Support TOCs with inline `code`.

### Changes
- Refactor the toc namepsace and fix code tags in the toc.

## [0.1.59] - 2017-10-21

### Highlights
- Prevent klipse from emitting empty html content when not enabled.

### Changes
- Bump version and deps.

## [0.1.58] - 2017-09-11

### Highlights
- Fix schemas to allow specifying toc type.

### Changes
- Bump version and deps.

## [0.1.57] - 2017-08-08

### Highlights
- Allow themes to have their own config.edn in order to specify
  theme-specific resources.
  
### Changes
- Also fix a bug where AsciiDoc has a syntax to embed Youtube but `/blog` would be prepended to the generated link.

## [0.1.56] - 2017-06-09

### Highlights
- Fix invalid RSS feed due to empty enclosure.

## [0.1.55] - 2017-04-10

### Highlights
- Add ability to optionally override `config.edn`.

### Changes
- Allow the overriding of config values by passing an optional argument to `compile-assets-timed`.
- Useful for environment specific configuration. (e.g. separate prod
  and dev reCaptcha public keys, or local vs production API endpoint
  urls).

## [0.1.54] - 2017-03-30

### Highlights
- Clean up Sass compilation.

### Changes
- The aim of this change is to separate compiling Sass files from
  copying them to the public directory, as they were combined in a way
  that made it hard to add more dirs. This release also fixes an issue
  with compiling Sass files under theme folders.

### Breaking
The following (potentially) breaking changes have been introduced: 
- The :sass-dest key is no longer used. (It never was. That was an
  oversight with the original Sass integration.)
- The :sass-src key now takes a vector of strings instead of just the
  one directory. This allows you to compile theme-specific scss files
  along with top level (under the templates folder) scss files.
- If you are using Sass in your Cryogen templates, be sure to include
  these directories in your resources section as the compiled css will
  be outputted in the same directory and will need to be copied over
  to the public folder.

## [0.1.53] - 2017-03-24

### Highlights
- Add schema definitions for configs.

### Breaking
- Some configuration values no longer have a default value provided
and some keys in the `config.edn` are now required. The documentation
has been updated here.

## [0.1.52] - 2017-02-01

### Highlights
- Add klipse integration.

### Changes
- Update deps.

## [0.1.51] - 2017-01-17

### Highlights
- Fix `dest-sass/sass-dest` key.

## [0.1.50] - 2017-01-16

### Highlights
- Pass both page/post params to the index template.

## [0.1.49] - 2017-01-16

### Highlights
- Fix a regression.

## [0.1.48] - 2017-01-16

### Highlights
- Update deps and version number.

## [0.1.26] - 2015-10-31

### Highlights
- Bump up dependencies and version number.

## [0.1.25] - 2015-08-26

### Highlights
- Bumped up `clj-rss`.

## [0.1.24] - 2015-07-08

### Highlights
- Only make the "prev" link in the second page when the second page
  actually exists.

## [0.1.23] - 2015-06-23

### Highlights
- Fixed `pubDate` not showing up in filtered rss feeds.

## [0.1.22] - 2015-06-14

### Highlights
- Config based themes.

## [0.1.20] - 2015-01-28

### Highlights
- Bumped up version.

## [0.1.19] - 2015-01-22

### Highlights
- Added `:content` key to grouped tags in order to fix filtered rss
  generation.

## [0.1.18] - 2015-01-17

### Highlights
- Bumped up version.

## [0.1.14] - 2015-01-09

### Highlights
- Merge pull request #5 from `Tankanow/add-asciidoc-support`.
- Add asciidoc support

[Unreleased]: https://github.com/cryogen-project/cryogen-core/compare/0.4.0...HEAD

[0.4.0]: https://github.com/cryogen-project/cryogen-core/compare/0.3.2...0.4.0
[0.3.2]: https://github.com/cryogen-project/cryogen-core/compare/0.3.1...0.3.2
[0.3.1]: https://github.com/cryogen-project/cryogen-core/compare/0.3.0...0.3.1
[0.3.0]: https://github.com/cryogen-project/cryogen-core/compare/0.2.3...0.3.0
[0.2.3]: https://github.com/cryogen-project/cryogen-core/compare/0.2.2...0.2.3
[0.2.2]: https://github.com/cryogen-project/cryogen-core/compare/0.2.1...0.2.2
[0.2.1]: https://github.com/cryogen-project/cryogen-core/compare/0.2.0...0.2.1
[0.2.0]: https://github.com/cryogen-project/cryogen-core/compare/0.1.68...0.2.0
[0.1.68]: https://github.com/cryogen-project/cryogen-core/compare/0.1.67...0.1.68
[0.1.67]: https://github.com/cryogen-project/cryogen-core/compare/0.1.66...0.1.67
[0.1.66]: https://github.com/cryogen-project/cryogen-core/compare/0.1.65...0.1.66
[0.1.65]: https://github.com/cryogen-project/cryogen-core/compare/0.1.64...0.1.65
[0.1.64]: https://github.com/cryogen-project/cryogen-core/compare/0.1.62...0.1.64
[0.1.62]: https://github.com/cryogen-project/cryogen-core/compare/0.1.61...0.1.62
[0.1.61]: https://github.com/cryogen-project/cryogen-core/compare/0.1.60...0.1.61
[0.1.60]: https://github.com/cryogen-project/cryogen-core/compare/0.1.59...0.1.60
[0.1.59]: https://github.com/cryogen-project/cryogen-core/compare/0.1.58...0.1.59
[0.1.58]: https://github.com/cryogen-project/cryogen-core/compare/0.1.57...0.1.58
[0.1.57]: https://github.com/cryogen-project/cryogen-core/compare/0.1.56...0.1.57
[0.1.56]: https://github.com/cryogen-project/cryogen-core/compare/0.1.55...0.1.56
[0.1.55]: https://github.com/cryogen-project/cryogen-core/compare/0.1.54...0.1.55
[0.1.54]: https://github.com/cryogen-project/cryogen-core/compare/0.1.53...0.1.54
[0.1.53]: https://github.com/cryogen-project/cryogen-core/compare/0.1.52...0.1.53
[0.1.52]: https://github.com/cryogen-project/cryogen-core/compare/0.1.51...0.1.52
[0.1.51]: https://github.com/cryogen-project/cryogen-core/compare/0.1.50...0.1.51
[0.1.50]: https://github.com/cryogen-project/cryogen-core/compare/0.1.49...0.1.50
[0.1.49]: https://github.com/cryogen-project/cryogen-core/compare/0.1.48...0.1.49
[0.1.48]: https://github.com/cryogen-project/cryogen-core/compare/0.1.26...0.1.48
[0.1.26]: https://github.com/cryogen-project/cryogen-core/compare/0.1.25...0.1.26
[0.1.25]: https://github.com/cryogen-project/cryogen-core/compare/0.1.24...0.1.25
[0.1.24]: https://github.com/cryogen-project/cryogen-core/compare/0.1.23...0.1.24
[0.1.23]: https://github.com/cryogen-project/cryogen-core/compare/0.1.22...0.1.23
[0.1.22]: https://github.com/cryogen-project/cryogen-core/compare/0.1.20...0.1.22
[0.1.20]: https://github.com/cryogen-project/cryogen-core/compare/0.1.19...0.1.20
[0.1.19]: https://github.com/cryogen-project/cryogen-core/compare/0.1.18...0.1.19
[0.1.18]: https://github.com/cryogen-project/cryogen-core/compare/0.1.14...0.1.18
[0.1.14]: https://github.com/cryogen-project/cryogen-core/compare/0.1.0...0.1.14

