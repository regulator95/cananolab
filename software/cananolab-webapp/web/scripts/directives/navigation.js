'use strict';
var app = angular.module('angularApp')

  .directive('navigation',function($location) {



    var link = function($scope, $element, $attrs) {
        $scope.url = $location.path();
        $scope.isEdit = 0;
        $scope.buttons = [];
        // $scope.edit = ['/editSample','/editComposition','/editCharacterization','/updatePublication'];
        $scope.edit = [{'url':'/editSample','name':'GENERAL INFO'},{'url':'/editComposition','name':'COMPOSITION'},{'url':'/editCharacterization','name':'CHARACTERIZATION'},{'url':'/updatePublication','name':'PUBLICATION'},{'url':'/editSynthesis','name':'SYNTHESIS'},{'url':'/setupCharacterization','alternativeUrl':'/editCharacterization','name':'CHARACTERIZATION'},{'url':'/editNanoMaterialEntity','alternativeUrl':'/editComposition','name':'COMPOSITION'},{'url':'/editFunctionalizingEntity','alternativeUrl':'/editComposition','name':'COMPOSITION'},{'url':'/editChemicalAssociation','alternativeUrl':'/editComposition','name':'COMPOSITION'},{'url':'/editCompositionFile','alternativeUrl':'/editComposition','name':'COMPOSITION'},{'url':'/editPublication','alternativeUrl':'/updatePublication','name':'PUBLICATION'},{'url':'/submitPublication','alternativeUrl':'/updatePublication','name':'PUBLICATION'},{'url':'/editSynthesisMaterials','alternativeUrl':'/editSynthesis','name':'SYNTHESIS'},{'url':'/editSynthesisFunctionalization','alternativeUrl':'/editSynthesis','name':'SYNTHESIS'},{'url':'/editSynthesisPurification','alternativeUrl':'/editSynthesis','name':'SYNTHESIS'}];
        $scope.view = [{'url':'/sample','name':'GENERAL INFO'},{'url':'/composition','name':'COMPOSITION'},{'url':'/characterization','name':'CHARACTERIZATION'},{'url':'/publication','name':'PUBLICATION'},{'url':'/synthesis','name':'SYNTHESIS'}];
        $scope.testClick = function(uri) {
            $location.path(uri).replace();
        }
        $scope.$watch(function() {
            return $location.path();

        },
        function(newValue, oldValue) {
            $scope.buttons = [];
            $scope.navTree = 0;
            for (var x=0;x<$scope.edit.length;x++) {
              if (newValue==$scope.edit[x].url) {
                $scope.isEdit=1;
                $scope.buttons = $scope.edit;
                $scope.url = newValue;
                // check if alternative url. this will highlight the alternative url as opposed to current url. //
                if ($scope.edit[x].alternativeUrl) {
                    $scope.url = $scope.edit[x].alternativeUrl;
                }
                $scope.navTree = 1;

              }
            };
            for (var x=0;x<$scope.view.length;x++) {
              if (newValue==$scope.view[x].url) {
                $scope.isEdit=0;
                $scope.buttons = $scope.view;  
                $scope.url = newValue;
                // check if alternative url. this will highlight the alternative url as opposed to current url. //
                if ($scope.view[x].alternativeUrl) {
                    $scope.url = $scope.view[x].alternativeUrl;
                }                
                $scope.navTree = 1;
              }
            }; 
        });
    };
 
    return {
        restrict: 'E',
        templateUrl:'scripts/directives/navigation.html',      
        link: link
    };
  });
