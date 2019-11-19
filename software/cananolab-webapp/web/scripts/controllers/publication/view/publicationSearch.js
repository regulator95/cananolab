'use strict';
var app = angular.module('angularApp')

  .controller('PublicationSearchCtrl', function (publicationService,navigationService,groupService,$rootScope,$scope,$http,$location) {
    $scope.searchPublicationForm = {};
    $scope.publicationData = publicationService.publicationData;    
    $rootScope.tabs = navigationService.get();
    $rootScope.groups = groupService.getGroups.data.get();   
    
    $scope.$on('$viewContentLoaded', function(){
      $http({method: 'GET', url: '/caNanoLab/rest/publication/setup'}).
      success(function(data, status, headers, config) {
        $scope.data = data;
        $scope.researchArea = {};
        $scope.searchPublicationForm.researchArea = [];
        for (var x=0;x<data.publicationResearchAreas.length;x++) {
          $scope.researchArea[$scope.data.publicationResearchAreas[x]]=false;
        }        
      }).
      error(function(data, status, headers, config) {
          // called asynchronously if an error occurs
          // or server returns response with an error status.
      $scope.message = data;
      });
    });     

       
    $scope.displayPubMed = function() {
      if ($scope.searchPublicationForm.category=='book chapter'||$scope.searchPublicationForm.category=='report') {
        $scope.pubMed = true;
      }
      else {
        $scope.pubMed = false;
      }
    };

    $scope.doSearch = function() {
      $scope.loader = true;
      for (var key in $scope.researchArea) {
        if ($scope.researchArea[key]) {
          $scope.searchPublicationForm.researchArea.push(key)          
        }

      }

      $http({method: 'POST', url: '/caNanoLab/rest/publication/searchPublication',data: $scope.searchPublicationForm}).
      success(function(data, status, headers, config) {
        // $rootScope.sampleData = data;
        $scope.publicationData.data = data;
        $location.path("/publicationResults").replace();

      }).
      error(function(data, status, headers, config) {
        // called asynchronously if an error occurs
        // or server returns response with an error status.
        // $rootScope.sampleData = data;
        $scope.loader = false;
        $scope.message = data;
      }); 
    };

    $scope.resetForm = function() {
      $scope.searchPublicationForm = {};
    };

  });
