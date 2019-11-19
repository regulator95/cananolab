'use strict';
var app = angular.module('angularApp')
.controller('LogoutCtrl', function ($rootScope, $scope, $location,$http) {

 $http({method: 'GET', url: '/caNanoLab/logout'}).
		    success(function(data, status, headers, config) {
		      // this callback will be called asynchronously
		      // when the response is available
		      //alert(data);
		      
		      $scope.message=data;
		      $rootScope.groups = null;
		      $rootScope.loggedInUser = '';
		      $location.path('/login?logout').replace();

		    }).
		    error(function(data, status, headers, config) {
		      // called asynchronously if an error occurs
		      // or server returns response with an error status.
		      //alert(data);
		      $scope.message=data;
  	});

});
