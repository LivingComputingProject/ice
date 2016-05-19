'use strict';

angular.module('ice.profile.controller', [])
    .controller('MessageController', function ($scope, $location, $cookieStore, $stateParams, Message) {
        var message = Message($cookieStore.get('sessionId'));
        var profileId = $stateParams.id;
        $location.path("profile/" + profileId + "/messages", false);
        message.query(function (result) {
            $scope.messages = result;
        });
    })
    .controller('ApiKeysController', function ($scope, $uibModal, Util) {
        $scope.apiKeys = undefined;

        // retrieve existing api keys for current user
        $scope.retrieveProfileApiKeys = function () {
            Util.get("rest/api-keys", function (result) {
                $scope.apiKeys = result.data;
            });
        };

        $scope.retrieveProfileApiKeys();

        $scope.openApiKeyRequest = function () {
            var modalInstance = $uibModal.open({
                templateUrl: 'scripts/profile/modal/api-key-request.html',
                controller: 'GenerateApiKeyController'
            })
        };

        $scope.deleteAPIKey = function (key) {
            Util.remove("rest/api-keys/" + key.id, key, function (result) {
                var idx = $scope.apiKeys.indexOf(key);
                if (idx >= 0)
                    $scope.apiKeys.splice(idx, 1);
            });
        }
    })
    .controller('GenerateApiKeyController', function ($scope, $uibModalInstance, Util) {
        $scope.apiKey = undefined;
        $scope.clientIdValidationError = undefined;
        $scope.errorCreatingKey = undefined;
        $scope.client = {};

        $scope.cancel = function () {
            $uibModalInstance.dismiss('cancel');
        };

        $scope.generateToken = function () {
            console.log($scope.client);
            if (!$scope.client.id) {
                $scope.clientIdValidationError = true;
                return;
            }

            var queryParams = {client_id: $scope.client.id};
            Util.post("/rest/api-keys", null, function (result) {
                $scope.apiKey = result;
            }, queryParams);
        }
    })
    .controller('ProfileEntryController', function ($scope, $location, $cookieStore, $stateParams, User, Entry) {
        var user = User($cookieStore.get("sessionId"));
        var profileId = $stateParams.id;

        $location.path("profile/" + profileId + "/entries", false);
        $scope.maxSize = 5;
        $scope.params = {userId: profileId, sort: "created", asc: false, currentPage: 1};

        user.getEntries($scope.params, function (result) {
            $scope.folder = result;
        });

        $scope.sort = function (sortType) {
            $scope.folder = null;
            // only change if switching to different sort
            $scope.params.asc = $scope.params.sort === sortType ? !$scope.params.asc : false;
            $scope.params.sort = sortType;
            $scope.params.offset = 0;
            user.getEntries($scope.params, function (result) {
                $scope.folder = result;
                $scope.params.currentPage = 1;
            }, function (error) {
                console.error(error);
            });
        };

        $scope.profileEntryPopupTemplate = "scripts/folder/template.html";

        $scope.tooltipDetails = function (entry) {
            $scope.currentTooltip = undefined;
            var sessionId = $cookieStore.get("sessionId");

            Entry(sessionId).tooltip({partId: entry.id},
                function (result) {
                    $scope.currentTooltip = result;
                }, function (error) {
                    console.error(error);
                });
        };

        $scope.userEntriesPageChanged = function () {
            $scope.loadingPage = true;
            $scope.params.offset = ($scope.params.currentPage - 1) * 15;
            user.getEntries($scope.params, function (result) {
                $scope.folder = result;
                $scope.loadingPage = false;
            }, function (error) {
                console.error(error);
            });
        };
    })
    .controller('ProfileController', function ($scope, $location, $cookieStore, $rootScope, $stateParams, User, Util) {
        $scope.showChangePassword = false;
        $scope.showEditProfile = false;
        $scope.showSendMessage = false;
        $scope.changePass = {};
        $scope.passwordChangeAllowed = false;

        // get settings
        Util.get("rest/config/PASSWORD_CHANGE_ALLOWED", function (result) {
            $scope.passwordChangeAllowed = (result.value.toLowerCase() === 'yes');
        });

        $scope.preferenceEntryDefaults = [
            {display: "Principal Investigator", id: "PRINCIPAL_INVESTIGATOR", help: "Enter Email or Name"},
            {display: "Funding Source", id: "FUNDING_SOURCE"}
        ];

        $scope.preferences = {};

        var user = User($cookieStore.get('sessionId'));
        var profileOption = $stateParams.option;
        var profileId = $scope.userId = $stateParams.id;

        $scope.savePreference = function (pref) {
            if (!$scope.preferences[pref.id]) {
                pref.invalid = true;
                return;
            }

            user.updatePreference({userId: profileId, value: $scope.preferences[pref.id]}, {preferenceKey: pref.id},
                function (result) {
                    pref.edit = false;
                });
        };

        var menuOptions = $scope.profileMenuOptions = [
            {
                url: 'scripts/profile/profile-information.html',
                display: 'Profile',
                selected: true,
                icon: 'fa-user',
                open: true
            },
            {
                id: 'prefs',
                url: 'scripts/profile/preferences.html',
                display: 'Settings',
                selected: false,
                icon: 'fa-cog'
            },
            {
                id: 'groups',
                url: 'scripts/profile/groups.html',
                display: 'Private Groups',
                selected: false,
                icon: 'fa-group'
            },
            {
                id: 'messages',
                url: 'scripts/profile/messages.html',
                display: 'Messages',
                selected: false,
                icon: 'fa-envelope-o'
            },
            {
                id: 'samples',
                url: 'scripts/profile/samples.html',
                display: 'Samples',
                selected: false,
                icon: 'fa-shopping-cart'
            },
            {
                id: 'entries',
                url: 'scripts/profile/entries.html',
                display: 'Entries',
                selected: false,
                icon: 'fa-th-list',
                open: true
            },
            {
                id: 'api-keys',
                url: 'scripts/profile/api-keys.html',
                display: 'API Keys',
                selected: false,
                icon: 'fa-key',
                open: true
            }
        ];

        $scope.showSelection = function (index) {
            var selectedOption = menuOptions[index];
            if (!selectedOption)
                return;

            var canViewSelected = selectedOption.open || user.isAdmin || ($scope.profile.email === $rootScope.user.email);
            if (!canViewSelected)
                return;

            angular.forEach(menuOptions, function (details) {
                details.selected = false;
            });
            selectedOption.selected = true;
            $scope.profileOptionSelection = menuOptions[index].url;
            if (selectedOption.id) {
                $location.path("profile/" + profileId + "/" + selectedOption.id);
            } else {
                $location.path("profile/" + profileId);
            }
        };

        // initialize view
        if (profileOption === undefined) {
            $scope.profileOptionSelection = menuOptions[0].url;
            menuOptions[0].selected = true;
        } else {
            menuOptions[0].selected = false;
            for (var i = 1; i < menuOptions.length; i += 1) {
                if (menuOptions[i].id === profileOption) {
                    $scope.profileOptionSelection = menuOptions[i].url;
                    menuOptions[i].selected = true;
                    break;
                }
            }

            if ($scope.profileOptionSelection === undefined) {
                $scope.profileOptionSelection = menuOptions[0].url;
                menuOptions[0].selected = true;
            }
        }

        // retrieve profile information from server
        user.query({userId: profileId}, function (result) {
            $scope.profile = result;
            user.getPreferences({userId: profileId}, function (prefs) {
                $scope.profile.preferences = prefs;
                if (prefs.preferences == undefined)
                    return;

                for (var i = 0; i < prefs.preferences.length; i += 1) {
                    $scope.preferences[prefs.preferences[i].key] = prefs.preferences[i].value;
                }
            });
        });

        $scope.editClick = function (message, profile, password) {
            $scope.showChangePassword = password;
            $scope.showEditProfile = profile;
            $scope.showSendMessage = message;
        };

        $scope.updatePassword = function () {
            var pass = $scope.changePass;

            //if (!$scope.changePass || $scope.changePass.current === undefined || !$scope.changePass.current.length) {
            //    $scope.changePasswordError = "Please enter your current password";
            //    $scope.currentError = true;
            //    return;
            //}

            // check new password value
            if (pass.new === undefined || pass.new.length === 0) {
                $scope.changePasswordError = "Please enter a new password";
                $scope.newPassError = true;
                return;
            }

            // check for new password confirm value
            if (pass.new2 === undefined || pass.new2.length === 0) {
                $scope.changePasswordError = "Please confirm the new password";
                $scope.newPass2Error = true;
                return;
            }

            // check for matching password values
            if (pass.new2 !== pass.new) {
                $scope.changePasswordError = "Passwords do not match";
                $scope.newPassError = true;
                $scope.newPass2Error = true;
                return;
            }

            var user = User($cookieStore.get("sessionId"));

            // validate existing password
            $scope.passwordChangeSuccess = undefined;
            $scope.changePasswordError = undefined;

            // server call
            user.changePassword({userId: $stateParams.id}, {password: pass.new},
                function (success) {
                    console.log("password change", success);
                    if (!success) {
                        $scope.changePasswordError = "There was an error changing the password";
                    } else {
                        $scope.passwordChangeSuccess = true;
                    }
                }, function (error) {
                    $scope.changePasswordError = "There was an error changing your password";
                });
        };

        $scope.updateProfile = function () {
            user.update({userId: profileId}, $scope.editProfile, function (result) {
                $scope.profile = result;
                $scope.editClick(false, false, false);
            });
        };

        $scope.switchtoEditMode = function () {
            $scope.editProfile = angular.copy($scope.profile);
        }
    })
    .controller('ProfileSamplesController', function ($scope, $cookieStore, $location, $stateParams, User) {
        $scope.maxSize = 15;
        $scope.params = {currentPage: 1};
        $scope.pendingSampleRequests = undefined;

        var user = User($cookieStore.get("sessionId"));
        var profileId = $stateParams.id;
        user.samples({userId: profileId},
            function (result) {
                $scope.userSamples = result;
            }, function (error) {
                console.error(error);
            });

        $scope.profileSamplesPageChanged = function () {
            $scope.loadingPage = true;
            $scope.offset = ($scope.params.currentPage - 1) * 15;
            user.samples({offset: $scope.offset}, {userId: profileId},
                function (result) {
                    $scope.userSamples = result;
                    $scope.loadingPage = false;
                }, function (error) {
                    console.error(error);
                    $scope.loadingPage = false;
                });
        }
    })
    .controller('ProfileGroupsController', function ($rootScope, $scope, $location, $cookieStore, $stateParams, User, Group) {
        var profileId = $stateParams.id;
        $location.path("profile/" + profileId + "/groups", false);
        $scope.selectedUsers = [];
        $scope.selectedRemoteUsers = [];
        $scope.myGroups = [];
        $scope.groupsIBelong = [];
        $scope.enteredUser = undefined;
        $scope.showCreateGroup = false;

        var user = User($cookieStore.get('sessionId'));
        var group = Group();

        // init: retrieve groups user belongs to and created
        user.getGroups({userId: profileId}, function (result) {
            angular.forEach(result, function (item) {
                if (item.ownerEmail && item.ownerEmail === $rootScope.user.email)
                    $scope.myGroups.push(item);
                else
                    $scope.groupsIBelong.push(item);
            });

            $scope.userGroups = result;
        });

        $scope.switchToEditMode = function (selectedGroup) {
            selectedGroup.edit = true;
            group.members({groupId: selectedGroup.id}, function (result) {
                selectedGroup.members = result;
            }, function (error) {
                console.error(error);
                selectedGroup.members = undefined;
            });
        };

        $scope.selectGroupUser = function (selectedGroup, user) {
            var index = selectedGroup.members.indexOf(user);
            if (index == -1)
                selectedGroup.members.push(user);
            else
                selectedGroup.members.splice(index, 1);
        };

        $scope.filterUsers = function (val) {
            if (!val) {
                $scope.userMatches = undefined;
                return;
            }

            $scope.filtering = true;
            user.filter({limit: 10, val: val},
                function (result) {
                    $scope.userMatches = result;
                    $scope.filtering = false;
                }, function (error) {
                    $scope.filtering = false;
                    $scope.userMatches = undefined;
                });
        };

        $scope.selectUser = function (user) {
            var index = $scope.selectedUsers.indexOf(user);
            if (index == -1)
                $scope.selectedUsers.push(user);
            else
                $scope.selectedUsers.splice(index, 1);
        };

        $scope.cancelGroupCreate = function () {
            $scope.selectedUsers = undefined;
            $scope.showCreateGroup = false;
            $scope.userMatches = undefined;
        };

        $scope.resetSelectedUsers = function () {
            $scope.selectedUsers = [];
        };

        $scope.createGroup = function (groupName, groupDescription) {
            $scope.newGroup = {label: groupName, description: groupDescription, members: $scope.selectedUsers};
            user.createGroup({userId: profileId}, $scope.newGroup, function (result) {
                $scope.myGroups.splice(0, 0, result);
                $scope.showCreateGroup = false;
            }, function (error) {
                console.error(error);
            })
        };

        $scope.updateGroup = function (selectedGroup) {
            group.update({groupId: selectedGroup.id}, selectedGroup, function (result) {
                selectedGroup.memberCount = selectedGroup.members.length;
                selectedGroup.edit = false;
            }, function (error) {
                console.error(error);
            });
        }
    })
;