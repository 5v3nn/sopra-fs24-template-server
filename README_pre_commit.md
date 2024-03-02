# Installation

- [https://pre-commit.com/#install](https://pre-commit.com/#install)

## Install pre-commit

```bash
pip install pre-commit
```

or

```bash
pip install pre-commit==3.6.2
```

Test if installed

```bash
$ pre-commit --version
pre-commit 3.6.2
```

Then, in the root (where the `.pre-commit-config.yaml` is)

```bash
$ pre-commit install
pre-commit installed at .git/hooks/pre-commit
```

## Run against all files

To test if it works, run against files:

```bash
$ pre-commit run --all-files
Check Yaml...............................................................Passed
Fix End of Files.........................................................Passed
Trim Trailing Whitespace.................................................Passed
Detect Private Key.......................................................Passed
clang-format.............................................................Passed
gradle check.............................................................Passed
gradle build.............................................................Passed
mdformat.................................................................Passed
```

Please note that the first run always takes a few minutes.

## When committing

If you commit and the pre-commit hook fails, it will modify the files. Just add
them again and then the commit should work. This is a safeguard that only valid
formatted code can be committed.

Otherwise, the pipeline will fail.
