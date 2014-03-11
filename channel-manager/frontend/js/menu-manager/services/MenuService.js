/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function () {
    "use strict";

    angular.module('hippo.channelManager.menuManager')

        .service('hippo.channelManager.menuManager.MenuService', [
            'hippo.channelManager.ConfigService',
            '$http',
            '$q',
            '$log',
            function (ConfigService, $http, $q) {
                var menuData = {
                        items: null
                    },
                    menuLoader = null;

                function menuServiceUrl(suffix) {
                    var url = ConfigService.apiUrlPrefix + '/' + ConfigService.menuId;
                    if (angular.isString(suffix)) {
                        url += './' + suffix;
                    }
                    return url;
                }

                function loadMenu() {
                    if (menuLoader === null) {
                        menuLoader = $q.defer();
                        $http.get(menuServiceUrl())
                            .success(function (response) {
                                menuData.items = response.data.items;
                                menuData.id = response.data.id;
                                menuLoader.resolve(menuData);
                            })
                            .error(function (error) {
                                menuLoader.reject(error);
                            });
                    }
                    return menuLoader.promise;
                }

                function findMenuItem(items, id) {
                    var found = _.findWhere(items, { id: id });
                    if (found === undefined && angular.isArray(items)) {
                        for (var i = 0, length = items.length; i < length && !found; i++) {
                            found = findMenuItem(items[i].items, id);
                        }
                    }
                    return found;
                }

                function findPathToMenuItem(parent, id) {
                    var found;
                    _.every(parent.items, function (item) {
                        if (item.id == id) {
                            found = [item];
                        } else if (item.items) {
                            found = findPathToMenuItem(item, id);
                        }
                        return found === undefined;
                    });
                    if (found) {
                        found.unshift(parent);
                    }
                    return found;
                }

                function getMenuItem(id) {
                    return findMenuItem(menuData.items, id);
                }

                function whenMenuLoaded(getResolved) {
                    var deferred = $q.defer();
                    loadMenu().then(
                        function() {
                            var resolved = angular.isFunction(getResolved) ? getResolved() : undefined;
                            deferred.resolve(resolved);
                        },
                        function(error) {
                            deferred.reject(error);
                        }
                    );
                    return deferred.promise;
                }

                function getSelectedItemIdBeforeDeletion(toBeDeletedItemId) {
                    var path = findPathToMenuItem(menuData, toBeDeletedItemId),
                        item, parent, items;
                    if (!path || path.length < 2) {
                        return undefined;
                    }

                    item = path.pop();
                    parent = path.pop();
                    items = parent.items;
                    if (items.length == 1) {
                        // item to delete has no siblings, so parent will be selected
                        return parent.id;
                    }
                    var itemIndex = _.indexOf(items, item);
                    if (itemIndex === 0) {
                        // Item to delete is first child, so select next child
                        return items[itemIndex + 1].id;
                    } else {
                        // Item to delete is not first child, so select previous child
                        return items[itemIndex - 1].id;
                    }
                }

                function post(url, body) {
                    return $http.post(url, body).success(function() {
                        menuLoader = null;
                        loadMenu();
                    }).error(function() {
                        menuLoader = null;
                        loadMenu();
                    });
                }

                return {

                    FIRST : 'first',
                    AFTER : 'after',

                    getMenu : function () {
                        return loadMenu();
                    },

                    getFirstMenuItemId : function () {
                        return whenMenuLoaded(function () {
                            return menuData.items[0].id;
                        });
                    },

                    getPathToMenuItem : function(menuItemId) {
                        return whenMenuLoaded(function () {
                            return findPathToMenuItem(menuData, menuItemId);
                        });
                    },

                    getMenuItem : function (menuItemId) {
                        return whenMenuLoaded(function () {
                            return getMenuItem(menuItemId);
                        });
                    },

                    saveMenuItem : function (menuItem) {
                        var deferred = $q.defer();
                        post(menuServiceUrl(), menuItem)
                            .success(function() {
                                    deferred.resolve();
                                })
                            .error(function (errorResponse) {
                                    deferred.reject(errorResponse);
                                });
                        return deferred.promise;
                    },

                    /**
                     * Create a new menu item.

                     * @param parentItemId When specified, the item will be created under the parent.
                     *                     Otherwise, the item will be created as a root item.
                     * @param menuItem The item to be created
                     * @param options item positioning details;
                     *      { position: <position> , siblingId: <sibling> }
                     *      with position either MenuService.FIRST or MenuService.AFTER.  The siblingId
                     *      is taken into account when the position is AFTER.
                     * @returns {promise|Promise.promise|Q.promise}
                     */
                    createMenuItem : function (parentItemId, menuItem, options) {
                        var deferred = $q.defer(), parentId = parentItemId;
                        if (parentId === undefined) {
                            parentId = ConfigService.menuId;
                        }
                        post(menuServiceUrl('create/' + parentId
                                                + (options ? '?position=' + options.position
                                                + (options.siblingId ? ('&sibling=' + options.siblingId) : '') : '')), menuItem)
                            .success(function(response) {
                                        menuItem.id = response.data;
                                        loadMenu().then(function() {
                                            deferred.resolve(response.data);
                                        }, function () {
                                            deferred.resolve(response.data);
                                        });
                                    })
                            .error(function (errorResponse) {
                                        deferred.reject(errorResponse);
                                    });
                        return deferred.promise;
                    },

                    deleteMenuItem : function (menuItemId) {
                        var selectedItemId = getSelectedItemIdBeforeDeletion(menuItemId);
                        var deferred = $q.defer();
                        post(menuServiceUrl('delete/' + menuItemId))
                            .success(function() {
                                deferred.resolve(selectedItemId);
                            })
                            .error(function (errorResponse) {
                                    deferred.reject(errorResponse);
                                });
                        return deferred.promise;
                    },

                    moveMenuItem : function (menuItemId, newParentId, newPosition) {
                        newParentId = (newParentId === '#') ? ConfigService.menuId : newParentId;
                        var url = menuServiceUrl('move/' + menuItemId + '/' + newParentId + '/' + newPosition );

                        var deferred = $q.defer();
                        post(url, {})
                            .success(function (data) {
                                deferred.resolve(data);
                            })
                            .error(function (errorResponse) {
                                deferred.reject(errorResponse);
                            });
                        return deferred.promise;
                    }
                };
            }

        ]);
}());
