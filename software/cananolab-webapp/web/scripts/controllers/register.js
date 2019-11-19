'use strict';

/**
 * @ngdoc function
 * @name angularApp.controller:MainCtrl
 * @description
 * # MainCtrl
 * Controller of the angularApp
 */
angular.module('angularApp')
  .controller('RegisterCtrl', function (navigationService,groupService, $rootScope, $scope, $http,$location) {
    $rootScope.tabs = navigationService.get();
	$rootScope.groups = groupService.getGroups.data.get();

	$scope.submitData = function(isValid) {
		$scope.submitted = true;
		if (isValid) { 
			//$http({method: 'POST', url: '/caNanoLab/rest/security/register', data: {"title":$scope.title,"firstName":$scope.firstName,"lastName":$scope.lastName,"email":$scope.email,"phone":$scope.phone,"organization":$scope.organization,"fax":$scope.fax,"comment":$scope.comment,"registerToUserList":$scope.registerToUserList}, headers: {'Content-Type':'application/json'}}).
	    	  $http({method: 'GET', url: '/caNanoLab/rest/security/register', params: {"title":$scope.title,"firstName":$scope.firstName,"lastName":$scope.lastName,"email":$scope.email,"phone":$scope.phone,"organization":$scope.organization,"fax":$scope.fax,"comment":$scope.comment,"registerToUserList":$scope.registerToUserList}}).
			    success(function(data, status, headers, config) {
			      // this callback will be called asynchronously
			      // when the response is available
			    	//$scope.message="Your registration request has been sent to the administrator for assignment of your User ID and Password. You should receive this information via e-mail within one business day from time of submission.";
			    	$location.search('message', 'Your registration request has been sent to the administrator for assignment of your User ID and Password. You should receive this information via e-mail within one business day from time of submission.').path('/message').replace();
			    }).
			    error(function(data, status, headers, config) {
			      // called asynchronously if an error occurs
			      // or server returns response with an error status.
			    	$scope.message=data;
	    	});
		};
	};
  });




