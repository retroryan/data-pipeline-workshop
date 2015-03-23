var app = angular.module('tweetMapApp', []);

app.factory('Twitter', function($http, $timeout) {

    var twitterService = {
        tweets: [],
        query: function (query) {
            $http({method: 'GET', url: '/search', params: {query: query}}).
                success(function (data) {
                    twitterService.tweets = data.statuses;
                });
        }
    };

    return twitterService;
});

app.controller('Search', function($scope, $http, $timeout, Twitter) {

    $scope.search = function() {
        Twitter.query($scope.query);
    };

});

app.controller('Tweets', function($scope, $http, $timeout, Twitter) {

    $scope.tweets = [];

    $scope.$watch(
        function() {
            return Twitter.tweets;
        },
        function(tweets) {
            $scope.tweets = tweets;
        }
    );

});