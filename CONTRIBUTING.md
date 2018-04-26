# Contribution guidelines
Project will probably be moved to ONAP Gerrit repository. Thus, please follow ONAP contribution guidelines.

## Merging

First of all let's avoid merge commits. To achieve it we should:

* rebase master changes to task branches,
* manually merge task branches to master with `--ff-only option`.

The last part needs some comment. In Gerrit master history is linear and easy to follow. That is probably the greatest (only? :wink:) advantage of Gerrit over GitLab. In GitLab EE it is possible to configure MRs so merge commits are avoided. However in GitLab CE it is not possible. To mitigate this, we should manually merge branches into master.

Some commands which you might find useful:

```shell
# 1. Update master
git checkout master
git fetch
git merge -ff-only

# 2. Rebase branch on fresh master
git checkout $BRANCH
git rebase -i master
git push --force

# 3. Merge branch
git checkout master
git merge --ff-only $BRANCH
git branch -d $BRANCH
git push origin --delete $BRANCH
git push

```

## Sign-off rules
Each commit has to be signed-off. By signing the commit you certify that you did follow rules defined in the document https://gerrit.onap.org/r/static/signoffrules.txt:

```
Sign your work

To improve tracking of who did what, especially with contributions that
can percolate to their final resting place in the code base through
several layers of maintainers, we've introduced a "sign-off" procedure
on all contributions submitted.

The sign-off is a simple line at the end of the explanation for the
contribution, which certifies that you wrote it or otherwise have the
right to pass it on as an open-source contribution. When you sign-off
your contribution you are stating the following:

    By making a contribution to this project, I certify that:

    (a) The contribution was created in whole or in part by me and I
        have the right to submit it under the open source license
        indicated in the file; or

    (b) The contribution is based upon previous work that, to the best
        of my knowledge, is covered under an appropriate open source
        license and I have the right under that license to submit that
        work with modifications, whether created in whole or in part
        by me, under the same open source license (unless I am
        permitted to submit under a different license), as indicated
        in the file; or

    (c) The contribution was provided directly to me by some other
        person who certified (a), (b) or (c) and I have not modified
        it.

    (d) I understand and agree that this project and the contribution
        are public and that a record of the contribution (including all
        personal information I submit with it, including my sign-off) is
        maintained indefinitely and may be redistributed consistent with
        this project or the open source license(s) involved.
```
