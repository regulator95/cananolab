'use strict';
var app = angular.module('angularApp')
	.controller('SampleDataAvailabilityCtrl', function ($rootScope,$scope,$http,$filter,$routeParams,$modalInstance,sampleId,sampleData,availabilityData,edit) {

	$scope.sampleId = sampleId;
	$scope.sampleData = sampleData;
	$scope.availabilityData = availabilityData;
     $scope.edit = edit;

	$scope.ok = function () {
		$modalInstance.close(sampleData);
	};

    // delete the data availability metric //
    // opens delete buttons if block is false, otherwise deletes or closes block //
    $scope.delete = function(val,block) {
        $scope.deleteBlock = true;
        // checks if it is a yes or no button or main delete button that calls method //
        if (block) {
            // called if yes button is pressed on delete block //
            if (val) {
                // $scope.poc.dirty = true;
                $scope.loader = true;
                $scope.loaderMessage = "Deleting";
                // $scope.message = 'Point of contact deleted';
                $http({method: 'POST', url: '/caNanoLab/rest/sample/deleteDataAvailability',data: $scope.sampleData}).
                    success(function(data, status, headers, config) {
                    	$scope.sampleData = data;
                        $modalInstance.close(data);
                    }).
                    error(function(data, status, headers, config) {
                        $scope.loader = false;
                        $scope.message = data;
                        $modalInstance.close();

                });
            }
            // this is no button. close the block //
            else {
                $scope.deleteBlock = false;
            };
        };
    };


    $scope.regenerate = function() {
           $scope.loader = true;
	      $scope.loaderMessage = "Regenerating";    	
		$http({method: 'GET', url: '/caNanoLab/rest/sample/regenerateDataAvailability',params: {"sampleId":$scope.sampleId}}).
            success(function(data, status, headers, config) {
                $modalInstance.close(data);

            }).
            error(function(data, status, headers, config) {
                $scope.message = data;
                $scope.loader = false;
            });
    }         

    });
