How to run HV-VES locally in development mode
-------------------------

	1. Generate certificates

		- go to `ssl` folder
		- execute `gen-certs.sh`
		- copy generated certificates into the `/etc/ves-hv/ssl` folder

	2. Run HV-VES

		- go to `development` folder
		- execute `docker-compose up -d`

	3. Basic commands

		- go to `development\bin` folder

		In mentioned folder you can find few scripts:

			- send messages: ./xnf-simulation.sh -v 6062 1000 VALID
			- check how many messages were sent to HV-VES: ./dcae-msgs.sh 
