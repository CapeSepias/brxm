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

    angular.module('hippo.channel.page')

        .service('hippo.channel.page.PageService', [
            'hippo.channel.ConfigService',
            '$http',
            '$q',
            function (ConfigService, $http, $q) {
                var pageService = {};

                function pageServiceUrl(suffix) {
                    var url = ConfigService.apiUrlPrefix + '/' + ConfigService.sitemapId;
                    if (angular.isString(suffix)) {
                        url += './' + suffix;
                    }
                    return url;
                }

                pageService.getPages = function () {
                    var deferred = $q.defer();

                    $http.get(pageServiceUrl('pages'))
                        .success(function (response) {
                            deferred.resolve(response.data.pages);
                        })
                        .error(function (error) {
                            deferred.reject(error);
                        });

                    return deferred.promise;
                };

                return pageService;
            }
        ]);
}());
