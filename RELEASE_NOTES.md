# Release Notes

This document contains high-level release notes. More details can be found in [milestones](https://github.com/searchisko/searchisko/issues/milestones) on GitHub.

## 1.0.2

- Content Push API returns warnings from data preprocessors
- Contributor record now contains full name replicated from profile. Useful for contributor normalization by name. 
  **IT IS NECESSARY** to remove `sys_contributors` index during upgrade, load new mapping for it, and then reindex Contributor data from persistence!

## 1.0.1

- Bug fix in `update_contributor_profile` task. [#65](https://github.com/searchisko/searchisko/issues/65)
- `jboss-developer` content provider configuration moved to different repo.
- Updates in DCP configuration.

## 1.0.0

- Initial release
