'use strict';
var app = angular.module('angularApp')

  .controller('TemplateCtrl', function ($route,$scope, $location, $http, keywordService) {
  $scope.keywordData = keywordService.keywordData;

  // create skip nav url //
  $scope.createSkipNavUrl = function() {
    var loc = $location.$$absUrl;
    if (loc.indexOf('maincontent')==-1) {
      loc = loc+='#maincontent';
    };
    return loc;
  };

  $scope.doKeywordSearch = function() {
    $scope.isSearching = true;
    $http({method: 'GET', url: '/caNanoLab/rest/customsearch/search?keyword=' + $scope.keyword_search_text}).

        success(function(data, status, headers, config) {
          $scope.keywordData.data = data;
          $location.path("/keywordSearchResults").replace();          
          $route.reload();
          $scope.isSearching = false;
        }).
        error(function(data, status, headers, config) {
          // called asynchronously if an error occurs
          // or server returns response with an error status.
         });
  };



  });

  $( document ).ready(function() {
  
  // bind a click event to the 'skip' link
  $(".skipLink").click(function(event){
  $("#maincontent").attr('tabindex', 0).on('blur focusout', function () {
      
      // when focus leaves this element, 
      // remove the tabindex attribute
      $(this).removeAttr('tabindex');
      
    }).focus(); // focus on the content
  });
  
});
