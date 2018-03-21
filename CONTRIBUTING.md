# Contributing

Looking to contribute something? Here's how you can help.

Please take a moment to review this guidelines to make the contribution process easy and effective.

## Using issue tracker

The issue tracker is the place for bug reports and feature requests.

Issue tracker isn't place for support, [our IRC](https://gitter.im/kotcity/Lobby "Our IRC") and [forum topic](https://community.simtropolis.com/forums/topic/74899-announcement-kotcity-an-open-source-city-simulator/ "Our topic on Simtropolis") is better place for it.

Don't spam in issue tracker, including "+1" or ":thumbsup:". Use [GitHub Reaction](https://github.com/blog/2119-add-reactions-to-pull-requests-issues-and-comments "GitHub Reaction") feature instead. We reserve the right to remove offending comments.

If you have write access to the repo, you should label issues with the right labels. For instance, bug issues, well should be labeled with "bug" label.

## Reporting bugs

To make the process easier, you should follow the guidelines:
1. Validate your issues. You should check your issues to ensure that the issue isn't occured by simple errors. For codes, you simply validate & lint your code.
2. Search your issue in issue tracker to ensure your bug isn't reported before.
3. Try using the repo's latest master or development branch and clean installation of the game.
4. Isolate the problem by taking screenshots as proofs of the bug.

Don't forget to add environment information, how to reproduce the bug and the expected outcome.

For bugs in the JavaFX or similar, you should report them to the representative issue trackers.

Example:

> # No building in newly-constructed fire dept
>
> After updating to latest
> version, the game shows no
> building in newly-constructed fire dept.
>
> Step to reproduce:
> 1. Open a city.
> 2. Plop a fire dept.
> 3. See what's happen.
>
> ![KotCity Screenshot](screenshot.jpg?raw=true "Screenshot of the game's UI and an example city")
>
> This is occured because a
> contributor accidentally 
> removes a line of code 
> handling texture loading of the
> building.

**Note:** title are not part of the report's content, so need to insert it to the separate text field.

## Making pull requests
Good pull requests helping the game to be better. But, they should be scoped and unrelated commit should not be included.

**Please ask first** before making a significant changes to the repo, such as code rewrites. Otherwise, you risk spending your time because the project denied your PRs.

Here's the process of creating a PR:
1. [Fork](https://help.github.com/fork-a-repo/) the project, clone your fork and configure the remotes.

   ```bash
   # Clone your fork of the repo into the current directory
   git clone https://github.com/<your-username>/bootstrap.git
   # Navigate to the newly cloned directory
   cd bootstrap
   # Assign the original repo to a remote called "upstream"
   git remote add upstream https://github.com/twbs/bootstrap.git
   ```

2. If you cloned a while ago, get the latest changes from upstream.

   ```bash
   git checkout v4-dev
   git pull upstream v4-dev
   ```

3. Create a new topic branch (off the main project development branch) to contain your feature, change or fix.

   ```bash
   git checkout -b <topic-branch-name>
   ```

4. Commit your changes in logical chunks. Your commit message should be short but clear.
5. Locally merge (or rebase) the upstream development branch into your topic branch:

   ```bash
   git pull [--rebase] upstream v4-dev
   ```

6. Push your topic branch up to your fork:

   ```bash
   git push origin <topic-branch-name>
   ```

7. [Open a Pull Request](https://help.github.com/articles/using-pull-requests/)
with a clear title and description against the master branch.

You should adhere to the Kotlin code guidelines for codes. Also, by submitting your work, you're agree to license your work under Apache License 2.0 for codes.

## Reporting security exploits
Due to their nature, security exploits aren't reported using issue tracker. Please send them privately to kotcity at zoho dot com.

*This document is adapted from the [Bootstrap contributing guidelines.](https://github.com/twbs/bootstrap/blob/master/CONTRIBUTING.md)*