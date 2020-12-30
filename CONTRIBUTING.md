# Contributing

We track bugs and issues on individual repos under the
[cryogen-project](https://github.com/cryogen-project) project.
There is also an active community on the `#cryogen` channel on the
[clojurians Slack](https://clojurians.slack.com).

## Bug reports and Issues

If you find a bug or if you want to suggest an enhancement, please
open an Issue on GitHub under the appropriate repo.

## Pull Requests

If you want to fix a bug or address an issue:

1. Preferably, fork the repo to your individual account on GitHub.
2. Create a feature branch on the repo.
3. Fix the bug/issue on that feature branch.
4. Push the feature branch.
5. Open a Pull Request (PR) to merge the change into this repo's master.

Please mention the open bug issue number within your PR if
applicable.

## Evaluating PRs

Here are some guidelines for evaluating PRs or feature requests.

* Is it necessary? Is there a reasonableh way to do it already? (E.g. using the existing customization hooks.)
* Is it simple? The more complex the higher the long-term maintenance cost for the maintainers.
* Is it popular? Have many people requested it?
* Does it enable future enhancements? Ideally in an orthogonal (i.e., decomplected) manner to other current and future features. Does the feature fit in with the existing conceptual and implementation architecture?
* What are alternatives to this particular solution? Have they been considered? Pros / cons evaluated?
* What future features are prohibited (or made very difficult) by this enhancement?
* Is the enhancement backwards compatible?
* How does the enhancement fit in with existing tests?  Are new tests needed?
* Sometimes interesting/useful features are requested but maybe not many people would actually use it. In this case a new "how to do x" article on the website should work nicely
