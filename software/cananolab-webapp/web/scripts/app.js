'use strict';

/**
 * @ngdoc overview
 * @name angularApp
 * @description
 * # angularApp
 *
 * Main module of the application.
 */

 function testInterceptor($rootScope, $interval, $injector) {
  function _getModal(){
     return $injector.get('$modal');
  }  

  function _getHeartbeat(){
     return $injector.get('$http');
  }    
  return {
    request: function(config) {
      // console.log(config)
      return config;
    },

    requestError: function(config) {
      return config;
    },

    response: function(res) {

      // cancels the timer //
      function cancelTimer() {
        $interval.cancel($rootScope.sessionTimer); // cancel existing timer if it exists //
      };

      // if tabs exist and modal is not open start a new timer //
      if ($rootScope.tabs && $rootScope.modalOpen == false) {
          cancelTimer(); // cancel any existing timer //

          $rootScope.sessionTimer = $interval(function() { // create new timer //
              if ($rootScope.tabs.userLoggedIn) {
                  $rootScope.modalOpen = true;
                  $rootScope.modalInstance = _getModal().open({
                      templateUrl: 'views/sessionTimeout.html',
                      controller: 'SessionTimeoutCtrl',
                      size: 'sm'            
                  }); 
              };

          }, 1500000, 1); // 25 minutes (25*60*1000 milliseconds) //  
      };

      return res;
    }

  }
};

var app = angular.module('angularApp', [
    'ngAnimate',
    'ngCookies',
    'ngResource',
    'ngRoute',
    'ngTable',
    'ngSanitize',
    'ngTouch',
    'ngGrid',
    'ui.bootstrap',
    'angularFileUpload']);

app.factory('testInterceptor', testInterceptor);

app.config(function ($routeProvider, $httpProvider) {

  $httpProvider.interceptors.push('testInterceptor');



  $httpProvider.defaults.useXDomain = true;  
  $httpProvider.defaults.cache = false;  
    if (!$httpProvider.defaults.headers.get) {
      $httpProvider.defaults.headers.get = {};
    };
    // disable IE ajax request caching
    $httpProvider.defaults.headers.get['If-Modified-Since'] = '0';
    $routeProvider
      .when('/', {
        templateUrl: 'views/main.html',
        controller: 'MainCtrl'
      })
     .when('/register', {
       templateUrl: 'views/register.html',
       controller: 'RegisterCtrl'
     }) 
     .when('/keywordSearchResults', {
       templateUrl: 'views/keywordSearch/keywordSearchResults.html',
       controller: 'KeywordSearchCtrl'
     })            
      .when('/workflow', {
        templateUrl: 'views/workflow.html',
        controller: 'WorkflowCtrl'
      })       
      .when('/searchSample', {
        templateUrl: 'views/sample/view/sampleSearch.html',
        controller: 'SampleSearchCtrl'
      }) 
      .when('/advancedSampleSearch', {
        templateUrl: 'views/sample/view/advancedSampleSearch.html',
        controller: 'AdvancedSampleSearchCtrl'
      })         
      .when('/sampleResults', {
        templateUrl: 'views/sample/view/sampleResults.html',
        controller: 'SampleResultsCtrl'
      })     
      .when('/advancedSampleResults', {
        templateUrl: 'views/sample/view/advancedSampleResults.html',
        controller: 'AdvancedSampleResultsCtrl'
      })
      .when('/dataColumnHeaderReset', {
        templateUrl: 'views/sample/view/dataColumnHeaderReset.html',
        controller: 'DataColumnHeaderResetCtrl'
      })
      .when('/noDatumColumn', {
        templateUrl: 'views/sample/view/noDatumColumn.html',
        controller: 'NoDatumColumnCtrl'
      })
      .when('/about', {
        templateUrl: 'views/about.html',
        controller: 'AboutCtrl'
      })
      .when('/login', {
        templateUrl: 'views/login.html',
        controller: 'LoginCtrl'
      })  
      .when('/logout', {
        templateUrl: 'views/logout.html',
        controller: 'LogoutCtrl'
      })  
      .when('/manageSample', {
       templateUrl: 'views/manageSample.html',
       controller: 'ManageSampleCtrl'
      }) 
      .when('/collaborationGroup', {
       templateUrl: 'views/collaborationGroup.html',
       controller: 'CollaborationGroupCtrl'
      })       
      .when('/manageGroups', {
       templateUrl: 'views/manageCommunity.html',
       controller: 'ManageCommunityCtrl'
      })       
      .when('/sample', {
       templateUrl: 'views/sample/view/viewSample.html',
       controller: 'IndSampleCtrl'
      })  
      .when('/sampleDelete', {
       templateUrl: 'views/sample/edit/sampleDelete.html',
       controller: 'DelSampleCtrl'
      })        
      .when('/submitSample', {
       templateUrl: 'views/sample/edit/editSample.html',
       controller: 'editSampleCtrl'
      })       
      .when('/editSample', {
       templateUrl: 'views/sample/edit/editSample.html',
       controller: 'editSampleCtrl'
      })       
      .when('/composition', {
       templateUrl: 'views/sample/view/composition.html',
       controller: 'CompositionCtrl'
      }) 
      .when('/characterization', {
       templateUrl: 'views/sample/view/characterization.html',
       controller: 'CharacterizationCtrl'
      }) 
      .when('/editCharacterization', {
       templateUrl: 'views/sample/edit/editCharacterization.html',
       controller: 'EditCharacterizationCtrl'
      })     
      .when('/setupCharacterization', {
       templateUrl: 'views/sample/edit/setupCharacterization.html',
       controller: 'SetupCharacterizationCtrl'
      })    
      .when('/synthesis', {
        templateUrl: 'views/sample/view/synthesis.html',
        controller: 'SynthesisCtrl'
       }) 
       .when('/editSynthesis', {
        templateUrl: 'views/sample/edit/synthesis/editSynthesis.html',
        controller: 'EditSynthesisCtrl'
       })   
       .when('/editSynthesisMaterials', {
        templateUrl: 'views/sample/edit/synthesis/editSynthesisMaterials.html',
        controller: 'EditSynthesisMaterialsCtrl'
       })   
       .when('/editSynthesisFunctionalization', {
        templateUrl: 'views/sample/edit/synthesis/editSynthesisFunctionalization.html',
        controller: 'EditSynthesisFunctionalizationCtrl'
       })   
       .when('/editSynthesisPurification', {
        templateUrl: 'views/sample/edit/synthesis/editSynthesisPurification.html',
        controller: 'EditSynthesisPurificationCtrl'
       })                                      
      .when('/publication', {
       templateUrl: 'views/sample/view/publication.html',
       controller: 'PublicationCtrl'
      }) 
      .when('/updatePublication', {
       templateUrl: 'views/sample/view/publication.html',
       controller: 'PublicationCtrl'
      })         
      .when('/managePublications', {
       templateUrl: 'views/publication/view/managePublications.html',
       controller: 'ManagePublicationCtrl'
      })                                                          
      .when('/searchPublication', {
        templateUrl: 'views/publication/view/publicationSearch.html',
        controller: 'PublicationSearchCtrl'
      })   
      .when('/searchSamplesByPublication', {
        templateUrl: 'views/publication/view/publicationSampleSearch.html',
        controller: 'PublicationSampleSearchCtrl'
      })      
      .when('/publicationResults', {
        templateUrl: 'views/publication/view/publicationResults.html',
        controller: 'PublicationResultsCtrl'
      })   
      .when('/publicationSampleInformation', {
        templateUrl: 'views/publication/view/publicationSampleInformation.html',
        controller: 'PublicationSampleInformationCtrl'
      })                  
      .when('/submitPublication', {
            templateUrl: 'views/publication/edit/editPublication.html',
            controller: 'EditPublicationCtrl'
      })
      .when('/editPublication', {
            templateUrl: 'views/publication/edit/editPublication.html',
            controller: 'EditPublicationCtrl'
      })
      .when('/message', {
            templateUrl: 'views/message.html',
            controller: 'MessageCtrl'
      })
      .when('/cloneSample', {
            templateUrl: 'views/sample/edit/cloneSample.html',
            controller: 'CloneSampleCtrl'
      })     
      .when('/manageSamples', {
            templateUrl: 'views/sample/view/manageSamples.html',
            controller: 'ManageSampleCtrl'
      })
      .when('/myWorkspace', {
            templateUrl: 'views/myWorkspace.html',
            controller: 'MyWorkspaceCtrl'
      })      
      .when('/searchProtocol', {
            templateUrl: 'views/protocol/view/protocolSearch.html',
            controller: 'ProtocolSearchCtrl'
      })
      .when('/protocolResults', {
            templateUrl: 'views/protocol/view/protocolResults.html',
            controller: 'ProtocolResultsCtrl'
      })  
      .when('/submitProtocol', {
            templateUrl: 'views/protocol/edit/editProtocol.html',
            controller: 'EditProtocolCtrl'
      })
      .when('/editProtocol', {
            templateUrl: 'views/protocol/edit/editProtocol.html',
            controller: 'EditProtocolCtrl'
      }) 
      .when('/manageProtocols', {
       templateUrl: 'views/protocol/view/manageProtocols.html',
       controller: 'ManageProtocolCtrl'
      })                                                          
      .when('/manageCuration', {
       templateUrl: 'views/curation/manageCuration.html',
       controller: 'ManageCurationCtrl'
      })     
      .when('/reviewData', {
            templateUrl: 'views/curation/reviewData.html',
            controller: 'ReviewDataCtrl'
      })      
      .when('/batchDataAvailability', {
            templateUrl: 'views/curation/batchDataAvailability.html',
            controller: 'BatchDataAvailabilityCtrl'
      })    
      .when('/batchDataResults', {
            templateUrl: 'views/curation/batchDataResults.html',
            controller: 'BatchDataResultsCtrl'
      }) 
      .when('/editComposition', {
            templateUrl: 'views/sample/edit/editComposition.html',
            controller: 'EditCompositionCtrl'
        })
      .when('/editNanoMaterialEntity', {
            templateUrl: 'views/sample/composition/nanomaterialEntity/editNanoMaterialEntity.html',
            controller: 'EditNanoEntityCtrl'
        })
        .when('/editFunctionalizingEntity', {
            templateUrl: 'views/sample/composition/functionalizingentity/editFunctionalizingEntity.html',
            controller: 'EditFuncEntityCtrl'
        })
        .when('/editChemicalAssociation', {
            templateUrl: 'views/sample/composition/editChemicalAssociation.html',
            controller: 'EditChemAssociationCtrl'
        })
        .when('/editCompositionFile', {
            templateUrl: 'views/sample/composition/editCompositionFile.html',
            controller: 'EditCompositionFileCtrl'
        })  
        .when('/myFavorites', {
            templateUrl: 'views/myFavorites.html',
            controller: 'MyFavoritesCtrl'
        })
        .when('/searchUser', {
            templateUrl: 'views/admin/view/userSearch.html',
            controller: 'UserSearchCtrl'
        })        
        .when('/userResults', {
              templateUrl: 'views/admin/view/userResults.html',
              controller: 'UserResultsCtrl'
        })  
        .when('/saveUser', {
              templateUrl: 'views/admin/edit/editUser.html',
              controller: 'EditUserCtrl'
        })
        .when('/editUser', {
              templateUrl: 'views/admin/edit/editUser.html',
              controller: 'EditUserCtrl'
        })
        .when('/resetPwd', {
              templateUrl: 'views/admin/edit/resetPwd.html',
              controller: 'EditUserCtrl'
        }) 
        .when('/manageUsers', {
         templateUrl: 'views/admin/view/manageUsers.html',
         controller: 'ManageUserCtrl'
        })  
      .otherwise({
        redirectTo: '/'
      });
  });

app.filter('newlines', function () {
    return function(text) {
      if(text)
          return text.replace(/\n/g, '<br/>').replace(/&amp;apos;/g, "'").replace(/&gt;/g, ">").replace(/&lt;/g, "<").replace(/&quot;/g,'"')
        return '';
    }
});

app.run(['$rootScope','$interval','$window','$location','$http', function($rootScope,$interval, $window,$location, $http) { 
  //Google Analytics URL creation to track # (hash) changes

    $rootScope.modalOpen = false;
    $rootScope.$on('$viewContentLoaded', function(event) {
      $window.ga('send', 'pageview', { page: $location.url() });
    });
}]);
